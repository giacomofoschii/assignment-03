package pcd.ass03.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.{LWWMap, LWWMapKey, SelfUniqueAddress}
import akka.cluster.ddata.typed.scaladsl.{DistributedData, Replicator}
import pcd.ass03.distributed.Messages

object PlayerRegistry:
  trait Command extends Messages
  private case class AddPlayer(playerId: String, nodeId: String) extends Command
  private case class RemovePlayer(playerId: String) extends Command
  private case class GetPlayers(replyTo: ActorRef[PlayersMap]) extends Command
  case class PlayersMap(players: Map[String, String]) extends Messages
  
  private case class InternalUpdateResponse(rsp: Replicator.UpdateResponse[LWWMap[String, String]]) extends Command
  private case class InternalGetResponse(rsp:Replicator.GetResponse[LWWMap[String, String]],
                                         replyTo: ActorRef[PlayersMap]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup: context =>
      implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress
      DistributedData.withReplicatorMessageAdapter[Command, LWWMap[String, String]]:
        replicator =>
          val PlayersKey = LWWMapKey[String, String]("players")

          Behaviors.receiveMessage:
            case AddPlayer(playerId, nodeId) =>
              replicator.askUpdate(
                askReplyTo => Replicator.Update(PlayersKey, LWWMap.empty[String, String],
                  Replicator.WriteLocal, askReplyTo)(_.put(node, playerId, nodeId)),
                InternalUpdateResponse.apply
              )
              Behaviors.same

            case RemovePlayer(playerId) =>
              replicator.askUpdate(
                askReplyTo => Replicator.Update(PlayersKey, LWWMap.empty[String, String],
                  Replicator.WriteLocal, askReplyTo)(_.remove(node, playerId)),
                InternalUpdateResponse.apply
              )
              Behaviors.same

            case GetPlayers(replyTo) => 
              replicator.askGet(
                askReplyTo => Replicator.Get(PlayersKey, Replicator.ReadLocal, askReplyTo),
                rsp => InternalGetResponse(rsp, replyTo)
              )
              Behaviors.same

            case InternalUpdateResponse(_) =>
              Behaviors.same

            case InternalGetResponse(rsp, replyTo) =>
              rsp match
                case res: Replicator.GetSuccess[LWWMap[String, String]] =>
                  val map = res.get(PlayersKey).entries
                  replyTo ! PlayersMap(map)
                case _ =>
                  replyTo ! PlayersMap(Map.empty)
              Behaviors.same