package pcd.ass03.actor

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern.*
import akka.util.Timeout

import scala.concurrent.duration.*
import scala.util.Success
import pcd.ass03.distributed.{MoveDirection, PlayerActorMessage, PlayerRegistered, RegisterPlayer, StartAI, UpdatePlayerPosition, UpdateView, WorldManagerMessage}
import pcd.ass03.view.LocalView
import pcd.ass03.model.{AIMovement, Player, World}

object PlayerActor:
  val PlayerServiceKey = ServiceKey[PlayerActorMessage]("PlayerActor")
  
  def apply(playerId: String, worldManager: ActorRef[WorldManagerMessage], isAI: Boolean = false):
    Behavior[PlayerActorMessage] = Behaviors.setup: context =>
      context.system.receptionist ! Receptionist.Register(PlayerServiceKey, context.self)
    
      implicit val timeout: Timeout = 3.seconds
      context.ask(worldManager, RegisterPlayer(playerId, _)):
        case Success(PlayerRegistered(player)) =>
          InitializePlayer(player)
        case _ => 
          FailedToRegister


  val localView = if !isAI then Some(new LocalView(playerId)) else None
      
      Behaviors.withTimers: timers =>
        if isAI then
          timers.startTimerAtFixedRate(StartAI, 100.millis)
          
        var currentPlayer: Option[Player] = None
        var currentWorld: Option[World] = None
        
        
        Behaviors.receiveMessage:
          case InitializePlayer(player) =>
            currentPlayer = Some(player)
            Behaviors.same

          case MoveDirection(dx, dy) =>
            currentPlayer.foreach: player =>
              val speed = 10.0
              val newX = (player.x + dx*speed).max(0).min(1000)
              val newY = (player.y + dy*speed).max(0).min(1000)
              worldManager ! UpdatePlayerPosition(player.id, newX, newY)
              
            Behaviors.same

          case UpdateView(world) =>
            currentWorld = Some(world)
            currentPlayer = world.playerById(playerId)
            localView.foreach(_.updateWorld(world))
            Behaviors.same

          case StartAI =>
            (currentPlayer, currentWorld) match
              case (Some(player), Some(world)) =>
                AIMovement.nearestFood(playerId, world).foreach: food =>
                  val dx = food.x - player.x
                  val dy = food.y - player.y
                  val distance = math.hypot(dx, dy)
                  if distance > 0 then
                    context.self ! MoveDirection(dx/distance, dy/distance)

              case _ => 
            Behaviors.same

          case FailedToRegister =>
            context.log.error(s"Failed to register player $playerId")
            Behaviors.stopped
            
  case class InitializePlayer(player: Player) extends PlayerActorMessage
  case object FailedToRegister extends PlayerActorMessage
    