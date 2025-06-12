package pcd.ass03.actor

import akka.actor.typed.receptionist._
import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.AskPattern.*
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Success, Failure}

import pcd.ass03.distributed._
import pcd.ass03.view.LocalView
import pcd.ass03.model._

object PlayerActor:
  val PlayerServiceKey = ServiceKey[PlayerActorMessage]("PlayerActor")
  
  def apply(playerId: String, worldManager: ActorRef[WorldManagerMessage], isAI: Boolean = false):
    Behavior[PlayerActorMessage] =
      Behaviors.setup: context =>
        context.log.info(s"Starting PlayerActor for $playerId (AI: $isAI)")

        Behaviors.withStash(100): stash =>
          def initializing(): Behavior[PlayerActorMessage] =
            context.log.debug(s"PlayerActor $playerId in initializing state")

            context.system.receptionist ! Receptionist.Register(PlayerServiceKey, context.self)
            context.ask(worldManager, RegisterPlayer(playerId, _)):
              case Success(PlayerRegistered(player)) =>
                context.log.info(s"Player $playerId succesfully registered")
                InitializeComplete(player)
              case Failure(ex) =>
                context.log.error(s"Failed to register plyer $playerId", ex)
                RegistrationFailed

          Behaviors.receiveMessage:
            case InitializeComplete(player) =>
              context.log.debug(s"Player $playerId initialization complete")
              stash.unstashAll(active(player, None))

            case RegistrationFailed =>
              context.log.error(s"Registration failed for $playerId, stopping")
              context.system.receptionist ! Receptionist.Deregister(PlayerServiceKey, context.self)
              Behaviors.stopped()

            case other =>
              stash.stash(other)
              Behaviors.same

        def active(currentPlayer: Player, localView: Option[LocalView]): Behavior[PlayerActorMessage] =
          val view = localView match
            case Some(v) => Some(v)
            case None if !isAI =>
              val newView = new LocalView(playerId)
              newView.setPlayerActor(context.self)
              Some(newView)
            case None => None

          val timers = if isAI then
            Behaviors.withTimers[PlayerActorMessage]: timers =>
              timers.startTimerAtFixedRate("ai-movement", StartAI, 100.millis)
              handleMessages(currentPlayer, view, Some(timers))
          else
            handleMessages(currentPlayer, view, None)

          timers

        def handleMessages(currentPlayer: Player, localView: Option[LocalView],
                           timers: Option[TimerScheduler[PlayerActorMessages]]): Behavior[PlayerActorMessages] =
          Behaviors.receiveMessage:
            case MoveDirection(dx,dy) =>
              val speed = 10.0
              val newX = (currentPlayer.x + dx * speed).max(0).min(1000) // Assumendo mondo 1000x1000
              val newY = (currentPlayer.y + dy * speed).max(0).min(1000)

              // Invia aggiornamento posizione al WorldManager (fire-and-forget)
              worldManager ! UpdatePlayerPosition(playerId, newX, newY)

              // Aggiorna stato locale
              val updatedPlayer = currentPlayer.copy(x = newX, y = newY)
              handleMessages(updatedPlayer, localView, timers)

            case UpdateView(world) =>
              localView.foreach: view =>
                try
                  view.updateWorld(world)
                catch
                  case ex: Exception =>
                    context.log.error(s"Error updating view for $playerId", ex)

              // Aggiorna il player corrente con i dati dal mondo
              val updatedPlayer = world.playerById(playerId).getOrElse(currentPlayer)
              handleMessages(updatedPlayer, localView, timers)

            case StartAI =>
              context.ask(worldManager, GetWorldState.apply):
                case Success(WorldState(world)) =>
                  AIMovement.nearestFood(playerId, world) match
                    case Some(food) =>
                      val dx = food.x - currentPlayer.x
                      val dy = food.y - currentPlayer.y
                      val distance = math.hypot(dx, dy)

                      if distance > 0 then
                        // Normalizza direzione e invia comando movimento
                        val normalizedDx = dx / distance
                        val normalizedDy = dy / distance
                        context.self ! MoveDirection(normalizedDx, normalizedDy)
                    case None =>
                  UpdateView(world) // Aggiorna anche la vista con i nuovi dati

                case Failure(ex) =>
                  ViewUpdateFailed(ex)

              Behaviors.same

            case ViewUpdateFailed(error) =>
              Behaviors.same

            case InitializeComplete(_) | RegistrationFailed =>
              Behaviors.same

        Behaviors
          .receiveSignal:
            case (context, PreRestart) =>
              context.log.info(s"PlayerActor $playerId is restarting")
              Behaviors.same

            case (context, PostStop) =>
              context.log.info(s"PlayerActor $playerId is stopping")
              // Cleanup: deregistra dal Receptionist
              context.system.receptionist ! Receptionist.Deregister(PlayerServiceKey, context.self)
              // Notifica WorldManager che il player sta uscendo
              worldManager ! UnregisterPlayer(playerId)
              Behaviors.same
          .narrow[PlayerActorMessage] // Assicura che il tipo sia corretto

        // Inizia con la fase di inizializzazione
        initializing()
  
  private case class InitializeComplete(player: Player) extends PlayerActorMessage
  private case object RegistrationFailed extends PlayerActorMessage
  private case class ViewUpdateFailed(error: Throwable) extends PlayerActorMessage
    