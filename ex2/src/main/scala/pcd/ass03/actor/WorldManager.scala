package pcd.ass03.actor

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.*
import akka.cluster.typed.*
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.util.Timeout
import com.typesafe.config.Config

import scala.concurrent.duration.*
import scala.util.{Failure, Success}
import scala.util.Random
import pcd.ass03.distributed.{AteFood, AtePlayer, FoodList, GetAllFood, GetWorldState, PlayerRegistered, RegisterPlayer, RemoveFood, Tick, UnregisterPlayer, UpdatePlayerPosition, WorldManagerMessage, WorldState}
import pcd.ass03.actor.{FoodManager, PlayerActor}
import pcd.ass03.model.{EatingManager, Food, Player, World}

object WorldManager:
  case class UpdateFood(foods: Seq[Food]) extends WorldManagerMessage
  case object NoOp extends WorldManagerMessage
  
  def apply(config: Config): Behavior[WorldManagerMessage] =
    Behaviors.supervise(worldManagerBehavior(config)).onFailure(SupervisorStrategy.restart)
    
  private def worldManagerBehavior(config: Config): Behavior[WorldManagerMessage] =
    Behaviors.setup: context =>
      Behaviors.withTimers: timers =>
        val width = config.getInt("game.world.width")
        val height = config.getInt("game.world.height")
        var world = World(width, height, Seq.empty, Seq.empty)

        timers.startTimerAtFixedRate(Tick, 30.millis)

        val foodManager = ClusterSingleton(context.system)
          .init(SingletonActor(FoodManager(config), "FoodManager"))

        Behaviors.receiveMessage:
          case RegisterPlayer(playerId, replyTo) =>
            context.log.info(s"Registering player $playerId")
            val player = Player(playerId, Random.nextInt(), Random.nextInt(), 120.0)
            world = world.copy(players = world.players :+ player)
            replyTo ! PlayerRegistered(player)
            Behaviors.same

          case UnregisterPlayer(playerId) =>
            context.log.info(s"Unregistering player $playerId")
            world = world.copy(players = world.players.filterNot(_.id == playerId))
            Behaviors.same

          case GetWorldState(replyTo) =>
            replyTo ! WorldState(world)
            Behaviors.same

          case UpdatePlayerPosition(playerId, x, y) =>
            world.playerById(playerId).foreach: player =>
              val updatedPlayer = player.copy(x = x, y = y)
              world = world.updatePlayer(updatedPlayer)
              checkCollisions(updatedPlayer, context)
            Behaviors.same

          case AteFood(playerId, foodId) =>
            world.playerById(playerId).foreach: player =>
              world.foods.find(_.id == foodId).foreach: food =>
                val grownPlayer = player.grow(food)
                world = world.updatePlayer(grownPlayer).copy(foods = world.foods.filterNot(_.id == foodId))
                foodManager ! RemoveFood(foodId)
            Behaviors.same

          case AtePlayer(killerId, victimId) =>
            (world.playerById(killerId), world.playerById(victimId)) match
              case (Some(killer), Some(victim)) =>
                val grownKiller = killer.grow(victim)
                world = world.updatePlayer(grownKiller).removePlayers(Seq(victim))
              case _ =>
                Behaviors.same
            Behaviors.same

          case Tick =>
            implicit val timeout: Timeout = 3.seconds
            context.ask(foodManager, GetAllFood.apply):
              case Success(FoodList(foods)) =>
                UpdateFood(foods)
              case _ =>
                NoOp

            broadcastWorldState(context)
            Behaviors.same

        def checkCollisions(player: Player, context: ActorContext[WorldManagerMessage]): Unit =
          world.foods.filter(food => EatingManager.canEatFood(player, food)).foreach: food =>
            context.self ! AteFood(player.id, food.id)
          world.playersExcludingSelf(player).filter(other => EatingManager.canEatPlayer(player, other))
            .foreach: other => context.self ! AtePlayer(player.id, other.id)

        def broadcastWorldState(context: ActorContext[WorldManagerMessage]): Unit =
          val receptionist = context.system.receptionist
          context.ask(receptionist, Receptionist.Find(PlayerActor.PlayerServiceKey, _))
          case Success(listing) =>
            listing.instances.foreach(_ ! UpdateView(world))
            NoOp
          case _ => NoOp