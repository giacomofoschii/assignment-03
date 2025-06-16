package pcd.ass03.actor

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.receptionist._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Success, Failure}

import pcd.ass03.distributed._
import pcd.ass03.model._
import pcd.ass03.view.DistributedLocalView

object PlayerActor:
  val PlayerServiceKey: ServiceKey[PlayerActorMessage] = ServiceKey[PlayerActorMessage]("PlayerActor")

  private case class InitializeComplete(player: Player) extends PlayerActorMessage
  private case object RegistrationFailed extends PlayerActorMessage
  private case class ViewUpdateFailed(error: Throwable) extends PlayerActorMessage
  
  def apply(playerId: String, worldManager: ActorRef[WorldManagerMessage], isAI: Boolean = false):
    Behavior[PlayerActorMessage] =
      Behaviors.setup { context =>
        Behaviors.withStash(100) { stash =>

          def initializing(): Behavior[PlayerActorMessage] = {
            context.system.receptionist ! Receptionist.Register(PlayerServiceKey, context.self)

            implicit val timeout: Timeout = 3.seconds
            context.ask(worldManager, RegisterPlayer(playerId, _)) {
              case Success(PlayerRegistered(player)) =>
                context.log.info(s"Player $playerId successfully registered")
                InitializeComplete(player)
              case Failure(ex) =>
                context.log.error(s"Failed to register player $playerId", ex)
                RegistrationFailed
            }

            Behaviors
              .receiveMessage[PlayerActorMessage] {
                case InitializeComplete(player) =>
                  context.log.debug(s"Player $playerId initialization complete")
                  stash.unstashAll(active(player, None))
  
                case RegistrationFailed =>
                  context.log.error(s"Registration failed for player $playerId")
                  context.system.receptionist ! Receptionist.Deregister(PlayerServiceKey, context.self)
                  Behaviors.stopped
  
                case other =>
                  stash.stash(other)
                  Behaviors.same
              }
              .receiveSignal {
                case (context, PostStop) =>
                  context.log.info(s"PlayerActor $playerId is stopping")
                  context.system.receptionist ! Receptionist.Deregister(PlayerServiceKey, context.self)
                  worldManager ! UnregisterPlayer(playerId)
                  Behaviors.same
              }
          }

          def active(currentPlayer: Player, localView: Option[DistributedLocalView]): Behavior[PlayerActorMessage] = {
            val view = localView match {
              case Some(v) => Some(v)
              case None if !isAI =>
                val newView = new DistributedLocalView(playerId)
                newView.setPlayerActor(context.self)
                Some(newView)
              case None => None
            }

            if isAI then
              Behaviors.withTimers[PlayerActorMessage] { timers =>
                timers.startTimerAtFixedRate("ai-movement", StartAI, 100.millis)
                handleMessages(currentPlayer, view)
              }
            else
              handleMessages(currentPlayer, view)
          }

          def handleMessages(currentPlayer: Player, localView: Option[DistributedLocalView]):
          Behavior[PlayerActorMessage] = {
            Behaviors.receiveMessage[PlayerActorMessage] {
              case MoveDirection(dx, dy) =>
                val speed = 10.0
                val newX = (currentPlayer.x + dx * speed).max(0).min(1000) // Assumendo mondo 1000x1000
                val newY = (currentPlayer.y + dy * speed).max(0).min(1000)

                // Invia aggiornamento posizione al WorldManager (fire-and-forget)
                worldManager ! UpdatePlayerPosition(playerId, newX, newY)

                // Aggiorna stato locale
                val updatedPlayer = currentPlayer.copy(x = newX, y = newY)
                handleMessages(updatedPlayer, localView)

              case UpdateView(world) =>
                localView.foreach(_.updateWorld(world))

                // Aggiorna il player corrente con i dati dal mondo
                val updatedPlayer = world.playerById(playerId).getOrElse(currentPlayer)
                handleMessages(updatedPlayer, localView)

              case StartAI =>
                implicit val timeout: Timeout = 3.seconds
                context.ask(worldManager, GetWorldState.apply) {
                  case Success(WorldState(world)) =>
                    AIMovement.nearestFood(playerId, world).foreach { food =>
                      val dx = food.x - currentPlayer.x
                      val dy = food.y - currentPlayer.y
                      val distance = math.hypot(dx, dy)

                      if distance > 0 then
                        // Normalizza direzione e invia comando movimento
                        val normalizedDx = dx / distance
                        val normalizedDy = dy / distance
                        context.self ! MoveDirection(normalizedDx, normalizedDy)
                    }
                    UpdateView(world) // Aggiorna anche la vista con i nuovi dati

                  case Failure(ex) =>
                    ViewUpdateFailed(ex)
                }

                Behaviors.same

              case ViewUpdateFailed(error) =>
                Behaviors.same

              case InitializeComplete(_) | RegistrationFailed =>
                Behaviors.same
            }
          }
          
          initializing()
        }
      }