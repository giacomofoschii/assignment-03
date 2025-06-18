package pcd.ass03.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.receptionist._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.util.{Success, Failure}

import pcd.ass03.distributed._
import pcd.ass03.model._
import pcd.ass03.view.DistributedLocalView
import pcd.ass03.GameConfig._

object PlayerActor:
  val PlayerServiceKey: ServiceKey[PlayerActorMessage] = ServiceKey[PlayerActorMessage]("PlayerActor")

  private case class InitializeComplete(player: Player) extends PlayerActorMessage
  private case object RegistrationFailed extends PlayerActorMessage
  private case class ViewUpdateFailed(error: Throwable) extends PlayerActorMessage
  
  def apply(playerId: String, worldManager: ActorRef[WorldManagerMessage], isAI: Boolean = false):
    Behavior[PlayerActorMessage] =
      Behaviors.setup : context =>
        Behaviors.withStash(100) : stash =>

          def initializing(retryCount: Int = 0): Behavior[PlayerActorMessage] =
            context.system.receptionist ! Receptionist.Register(PlayerServiceKey, context.self)

            implicit val timeout: Timeout = ThreeSeconds
            context.ask(worldManager, RegisterPlayer(playerId, _)):
              case Success(PlayerRegistered(player)) =>
                println(s"Player $playerId successfully registered")
                InitializeComplete(player)
              case _ =>
                println(s"Failed to register player $playerId")
                if retryCount < 3 && isAI then
                  Thread.sleep(1000)
                  InitializeComplete(null)
                else
                  RegistrationFailed

            Behaviors
              .receiveMessage[PlayerActorMessage]:
                case InitializeComplete(player) =>
                  if player != null then
                    println(s"Player $playerId initialization complete")
                    stash.unstashAll(active(player, None))
                  else if isAI && retryCount < 3 then
                    initializing(retryCount + 1)
                  else
                    context.system.receptionist ! Receptionist.Deregister(PlayerServiceKey, context.self)
                    Behaviors.stopped

                case RegistrationFailed =>
                  println(s"Registration failed for player $playerId")
                  context.system.receptionist ! Receptionist.Deregister(PlayerServiceKey, context.self)
                  Behaviors.stopped

                case other =>
                  stash.stash(other)
                  Behaviors.same

              .receiveSignal:
                case (context, PostStop) =>
                  context.system.receptionist ! Receptionist.Deregister(PlayerServiceKey, context.self)
                  worldManager ! UnregisterPlayer(playerId)
                  Behaviors.same

          def active(currentPlayer: Player, localView: Option[DistributedLocalView]): Behavior[PlayerActorMessage] =
            val view = localView match
              case Some(v) => Some(v)
              case None if !isAI =>
                val newView = new DistributedLocalView(playerId)
                newView.setPlayerActor(context.self)
                Some(newView)
              case None => None

            if isAI then
              Behaviors.withTimers[PlayerActorMessage] : timers =>
                timers.startTimerAtFixedRate("ai-movement", StartAI, HundredMillis)
                handleMessages(currentPlayer, view, timers)
            else
              handleMessages(currentPlayer, view, null)

          def handleMessages(currentPlayer: Player, localView: Option[DistributedLocalView],
                             timers: TimerScheduler[PlayerActorMessage]): Behavior[PlayerActorMessage] =
            Behaviors.receiveMessage[PlayerActorMessage] :
              case MoveDirection(dx, dy) =>
                val speed = DefaultSpeed
                val newX = (currentPlayer.x + dx * speed).max(0).min(WorldWidth)
                val newY = (currentPlayer.y + dy * speed).max(0).min(WorldHeight)

                // Send position update to the WorldManager (fire-and-forget)
                worldManager ! UpdatePlayerPosition(playerId, newX, newY)

                // Update the local state
                val updatedPlayer = currentPlayer.copy(x = newX, y = newY)
                handleMessages(updatedPlayer, localView, timers)

              case UpdateView(world) =>
                localView.foreach(_.updateWorld(world))
                // Update the current player with data from the world
                val updatedPlayer = world.playerById(playerId).getOrElse(currentPlayer)
                handleMessages(updatedPlayer, localView, timers)

              case PlayerDied(id) if id == playerId =>
                println(s"Player $playerId has died.")
                if isAI && timers != null then
                  timers.cancel("ai-movement") // Stop AI movement timer on death

                if isAI then
                  println(s"AI player $playerId died, waiting for respawn...")
                  Behaviors.withTimers[PlayerActorMessage] { respawnTimers =>
                    respawnTimers.startSingleTimer("respawn", Respawn, FiveSeconds)
                    waitingForRespawn(localView, worldManager, isAI)
                  }
                else
                  println(s"Player $playerId died, showing respawn dialog...")
                  localView.foreach(_.showRespawnDialog())
                  waitingForRespawn(localView, worldManager, isAI)

              case StartAI =>
                implicit val timeout: Timeout = ThreeSeconds
                context.ask(worldManager, GetWorldState.apply) :
                  case Success(WorldState(world)) =>
                    AIMovement.nearestFood(playerId, world).foreach : food =>
                      val dx = food.x - currentPlayer.x
                      val dy = food.y - currentPlayer.y
                      val distance = math.hypot(dx, dy)

                      if distance > 0 then
                        // Normalized the direction vector
                        val normalizedDx = dx / distance
                        val normalizedDy = dy / distance
                        context.self ! MoveDirection(normalizedDx, normalizedDy)

                    UpdateView(world) // Update also the view with the current world state

                  case Failure(ex) =>
                    ViewUpdateFailed(ex)

                Behaviors.same

              case ViewUpdateFailed(error) =>
                println(s"Failed to update view: ${error.getMessage}")
                Behaviors.same

              case InitializeComplete(_) | RegistrationFailed =>
                println(s"Unexpected message during active state for player $playerId")
                Behaviors.same

              case PlayerDied(_) =>
                println(s"Player $playerId is already dead, ignoring death message.")
                Behaviors.same

              case Respawn =>
                println(s"Player $playerId is already active, ignoring respawn request.")
                Behaviors.same

          def waitingForRespawn(localView: Option[DistributedLocalView], worldManager: ActorRef[WorldManagerMessage],
                                isAI: Boolean): Behavior[PlayerActorMessage] =
            Behaviors.withTimers : timers =>
              Behaviors.receiveMessage :
                case Respawn =>
                  println(s"Player $playerId is respawning...")
                  implicit val timeout: Timeout = ThreeSeconds
                  context.ask(worldManager, RegisterPlayer(playerId, _)) :
                    case Success(PlayerRegistered(player)) =>
                      println(s"Player $playerId respawned successfully")
                      localView.foreach(_.setActive(true))
                      InitializeComplete(player)
                    case _ =>
                      if isAI then
                        println(s"AI player $playerId failed to respawn, retrying...")
                        timers.startSingleTimer("retry-registration", Respawn, ThreeSeconds)
                      RegistrationFailed

                  Behaviors.same

                case InitializeComplete(player) =>
                  println(s"Player $playerId respawned successfully, initializing")
                  active(player, localView)

                case UpdateView(world) =>
                  println(s"Received world update while waiting for respawn for player $playerId")
                  localView.foreach(_.updateWorld(world))
                  Behaviors.same

                case PlayerDied(_) =>
                  println(s"Player $playerId is dead and waiting for respawn, ignoring other death messages.")
                  Behaviors.same // Ignore further death messages while waiting to respawn

                case StartAI =>
                  println(s"AI player $playerId is waiting for respawn, ignoring AI movement.")
                  Behaviors.same

                case MoveDirection(_, _) =>
                  println(s"Player $playerId is dead, ignoring moveDirection messages.")
                  Behaviors.same

                case other =>
                  println(s"Unexpected message while waiting for respawn: $other")
                  Behaviors.same

          initializing()

