package pcd.ass03.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.*
import com.typesafe.config.Config

import scala.concurrent.duration.*
import scala.util.Random
import pcd.ass03.model.Food
import pcd.ass03.distributed.{FoodList, FoodManagerMessage, GenerateFood, GetAllFood, RemoveFood}
import pcd.ass03.GameConfig.TwoSeconds

object FoodManager:
  def apply(config: Config) : Behavior[FoodManagerMessage] =
    Behaviors.setup: context =>
      Behaviors.withTimers: timers =>
        val width = config.getInt("game.world.width")
        val height = config.getInt("game.world.height")
        val maxFood = config.getInt("game.food.max")
        var foodList = (1 to maxFood).map(i => Food(s"f$i", Random.nextInt(width), Random.nextInt(height)))

        var foodCounter = maxFood
        
        timers.startTimerAtFixedRate(GenerateFood(5), TwoSeconds)
        
        Behaviors.receiveMessage:
          case GenerateFood(count) =>
            if foodList.size < maxFood then
              val newFoods = (1 to math.min(count, maxFood - foodList.size)).map: _ =>
                foodCounter += 1
                Food(s"f$foodCounter", Random.nextInt(width), Random.nextInt(height))
              foodList = foodList ++ newFoods
            Behaviors.same

          case RemoveFood(foodId) =>
            foodList = foodList.filterNot(_.id == foodId)
            Behaviors.same

          case GetAllFood(replyTo) =>
            replyTo ! FoodList(foodList)
            Behaviors.same
