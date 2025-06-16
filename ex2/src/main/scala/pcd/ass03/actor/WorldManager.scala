package pcd.ass03.actor

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.typed._
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.util.{Success, Failure, Random}

import pcd.ass03.distributed._
import pcd.ass03.model._

object WorldManager:
  private case class UpdateFood(foods: Seq[Food]) extends WorldManagerMessage
  private case object NoOp extends WorldManagerMessage

  def apply(config: Config): Behavior[WorldManagerMessage] =
    Behaviors.supervise(worldManagerBehavior(config))
      .onFailure(SupervisorStrategy.restart)

  private def worldManagerBehavior(config: Config): Behavior[WorldManagerMessage] =
    Behaviors.setup: context =>
      context.log.info("Starting WorldManager")

      Behaviors.withTimers: timers =>
        val width = config.getInt("game.world.width")
        val height = config.getInt("game.world.height")
        var world = World(width, height, Seq.empty, Seq.empty)

        timers.startTimerAtFixedRate(Tick, 30.millis)

        val foodManager = ClusterSingleton(context.system).init(
          SingletonActor(FoodManager(config), "FoodManager")
        )

        def checkCollisions(player: Player, context: ActorContext[WorldManagerMessage]): Unit =
          // Check food collisions
          world.foods.filter(food => EatingManager.canEatFood(player, food)).foreach: food =>
            context.self ! AteFood(player.id, food.id)

          // Check player collisions
          world.playersExcludingSelf(player)
            .filter(other => EatingManager.canEatPlayer(player, other))
            .foreach: other =>
              context.self ! AtePlayer(player.id, other.id)

        Behaviors.receiveMessage:
          case RegisterPlayer(playerId, replyTo) =>
            context.log.info(s"Registering player $playerId")
            val player = Player(
              playerId,
              Random.nextInt(width),
              Random.nextInt(height),
              120.0
            )
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
                world = world.updatePlayer(grownPlayer).copy(
                  foods = world.foods.filterNot(_.id == foodId)
                )
                foodManager ! RemoveFood(foodId)
            Behaviors.same

          case AtePlayer(killerId, victimId) =>
            (world.playerById(killerId), world.playerById(victimId)) match
              case (Some(killer), Some(victim)) =>
                val grownKiller = killer.grow(victim)
                world = world.updatePlayer(grownKiller).removePlayers(Seq(victim))
                Behaviors.same
              case _ => 
                Behaviors.same// Ignore

          case Tick =>
            // Aggiorna cibo
            implicit val timeout: akka.util.Timeout = 100.millis
            context.ask(foodManager, GetAllFood.apply):
              case Success(FoodList(foods)) =>
                UpdateFood(foods)
              case Failure(_) =>
                NoOp

            // Broadcast stato mondo
            context.ask(context.system.receptionist, Receptionist.Find(PlayerActor.PlayerServiceKey, _)):
              case Success(listing: Receptionist.Listing) =>
                listing.serviceInstances(PlayerActor.PlayerServiceKey).foreach: actorRef =>
                  actorRef ! UpdateView(world)
                NoOp
              case Failure(_) =>
                NoOp

            Behaviors.same

          case UpdateFood(foods) =>
            world = world.copy(foods = foods)
            Behaviors.same

          case NoOp =>
            Behaviors.same
  