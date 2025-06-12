package pcd.ass03.distributed

import akka.actor.typed._
import akka.actor.typed.scaladsl._

import pcd.ass03.actor._

// Companion object con utility methods
object PlayerActorUtils:

  // Factory method per creare un player umano
  def createHumanPlayer(
                         playerId: String,
                         worldManager: ActorRef[WorldManagerMessage],
                         context: ActorContext[_]
                       ): ActorRef[PlayerActorMessage] =
    context.spawn(
      PlayerActor(playerId, worldManager, isAI = false),
      s"player-$playerId"
    )

  // Factory method per creare un AI player
  def createAIPlayer(
                      playerId: String,
                      worldManager: ActorRef[WorldManagerMessage],
                      context: ActorContext[_]
                    ): ActorRef[PlayerActorMessage] =
    context.spawn(
      PlayerActor(playerId, worldManager, isAI = true),
      s"ai-player-$playerId"
    )

  // Utility per creare multipli AI players
  def createMultipleAIPlayers(
                               count: Int,
                               worldManager: ActorRef[WorldManagerMessage],
                               context: ActorContext[_]
                             ): Seq[ActorRef[PlayerActorMessage]] =
    (1 to count).map: i =>
      createAIPlayer(s"AI-$i", worldManager, context)
