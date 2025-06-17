package pcd.ass03.distributed

import akka.actor.typed.ActorRef

import pcd.ass03.model.{Player, World, Food}

/** Tag interface for all messages sends by actors */
trait Messages

trait GameMessage extends Messages

trait WorldManagerMessage extends GameMessage
case class RegisterPlayer(playerId: String, ref: ActorRef[PlayerRegistered]) extends WorldManagerMessage
case class UnregisterPlayer(playerId: String) extends WorldManagerMessage
case class GetWorldState(replyTo: ActorRef[WorldState]) extends WorldManagerMessage
case class UpdatePlayerPosition(playerId: String, x: Double, y: Double) extends WorldManagerMessage
case class AteFood(playerId: String, foodId: String) extends WorldManagerMessage
case class AtePlayer(killerId: String, victimId: String) extends WorldManagerMessage
case object Tick extends WorldManagerMessage
case class WorldState (world: World) extends GameMessage
case class PlayerRegistered(player: Player) extends GameMessage

trait FoodManagerMessage extends GameMessage
case class GenerateFood(count: Int) extends FoodManagerMessage
case class RemoveFood(foodId: String) extends FoodManagerMessage
case class GetAllFood(replyTo: ActorRef[FoodList]) extends FoodManagerMessage
case class FoodList(foods: Seq[Food]) extends GameMessage

trait PlayerActorMessage extends GameMessage
case class MoveDirection(dx: Double, dy: Double) extends PlayerActorMessage
case class UpdateView(world: World) extends PlayerActorMessage
case object StartAI extends PlayerActorMessage

