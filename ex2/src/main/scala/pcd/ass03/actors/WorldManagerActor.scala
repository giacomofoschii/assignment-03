package pcd.ass03.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.typed._
import com.typesafe.config.Config

import scala.util.{Success, Failure, Random}

import pcd.ass03.distributed._
import pcd.ass03.model._
import pcd.ass03.GameConfig._

object WorldManagerActor:
  private case class UpdateFood(foods: Seq[Food]) extends WorldManagerMessage
  private case object NoOp extends WorldManagerMessage
  private case object InitializeFoodManager extends WorldManagerMessage
  private case class PlayersUpdate(player: Set[ActorRef[PlayerActorMessage]]) extends WorldManagerMessage

  def apply(config: Config): Behavior[WorldManagerMessage] =
    Behaviors.supervise(worldManagerBehavior(config))
      .onFailure(SupervisorStrategy.restart)

  private def worldManagerBehavior(config: Config): Behavior[WorldManagerMessage] =
    Behaviors.setup: context =>

      Behaviors.withTimers: timers =>
        val width = WorldWidth
        val height = WorldHeight
        var world = World(width, height, Seq.empty, Seq.empty)
        var registeredPlayers = Set.empty[ActorRef[PlayerActorMessage]]

        timers.startSingleTimer(InitializeFoodManager, TwoSeconds)

        val foodManagerProxy = ClusterSingleton(context.system).init(
          SingletonActor(FoodManagerActor(config), "FoodManager")
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
          case InitializeFoodManager =>
            // Start the Tick timer only after initialization
            timers.startTimerAtFixedRate(Tick, FiftyMillis)
            Behaviors.same

          case RegisterPlayer(playerId, replyTo) =>
            println(s"Registering player $playerId")
            val player = Player(
              playerId,
              Random.nextInt(width),
              Random.nextInt(height),
              PlayerMass
            )
            world = world.copy(players = world.players :+ player)
            replyTo ! PlayerRegistered(player)
            Behaviors.same

          case UnregisterPlayer(playerId) =>
            println(s"Unregistering player $playerId")
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
                foodManagerProxy ! RemoveFood(foodId)
            Behaviors.same

          case AtePlayer(killerId, victimId) =>
            println(s"Player $killerId ate player $victimId")
            (world.playerById(killerId), world.playerById(victimId)) match
              case (Some(killer), Some(victim)) =>
                val grownKiller = killer.grow(victim)
                world = world.updatePlayer(grownKiller).removePlayers(Seq(victim))

                implicit val Timeout: akka.util.Timeout = HundredMillis
                context.ask(context.system.receptionist, Receptionist.Find(PlayerActor.PlayerServiceKey, _)):
                  case Success(listing: Receptionist.Listing) =>
                    println(s"Broadcasting death of player $victimId to all players")
                    listing.serviceInstances(PlayerActor.PlayerServiceKey).foreach: playerRef =>
                      val pathname = playerRef.path.name
                      if playerRef.path.name == s"Player-$victimId" ||
                         playerRef.path.name == s"AI-Player-$victimId" then
                        println(s"Notifying player $pathname of death")
                        playerRef ! PlayerDied(victimId)
                    NoOp
                  case _ =>
                    println(s"Could not find player $killerId or $victimId in world")
                    NoOp

                Behaviors.same
              case _ =>
                println(s"Could not find killer: $killerId or victim: $victimId in world")
                Behaviors.same

          case Tick =>
            implicit val timeout: akka.util.Timeout = HundredMillis
            context.ask(foodManagerProxy, GetAllFood.apply):
              case Success(FoodList(foods)) =>
                UpdateFood(foods)
              case Failure(_) =>
                NoOp

            // Broadcast world state
            context.ask(context.system.receptionist, Receptionist.Find(PlayerActor.PlayerServiceKey, _)):
              case Success(listing: Receptionist.Listing) =>
                val activePlayers = listing.serviceInstances(PlayerActor.PlayerServiceKey)
                registeredPlayers = activePlayers
                activePlayers.foreach: actorRef =>
                  val pathName = actorRef.path.name
                  val playerStillExist = world.players.exists: p =>
                    pathName == s"Player-${p.id}" || pathName == s"AI-Player-${p.id}"

                  if playerStillExist then
                    actorRef ! UpdateView(world)

                PlayersUpdate(activePlayers)
              case _ =>
                NoOp

            Behaviors.same

          case UpdateFood(foods) =>
            world = world.copy(foods = foods)
            Behaviors.same

          case PlayersUpdate(players) =>
            registeredPlayers = players
            Behaviors.same

          case NoOp =>
            Behaviors.same
