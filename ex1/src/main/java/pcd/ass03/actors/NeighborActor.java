package pcd.ass03.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import pcd.ass03.Boid;
import pcd.ass03.model.BoidState;
import pcd.ass03.protocols.BoidProtocol;
import pcd.ass03.protocols.NeighborProtocol;
import pcd.ass03.utils.P2d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NeighborActor {

    public NeighborActor(ActorContext<NeighborProtocol.Command> context) {
    }

    public static Behavior<NeighborProtocol.Command> create() {
        return Behaviors.setup(context -> new NeighborActor(context).behavior());
    }

    private final Map<String, P2d> positions = new HashMap<>();

    private Behavior<NeighborProtocol.Command> behavior() {
        return Behaviors.receive(NeighborProtocol.Command.class)
            .onMessage(NeighborProtocol.GetNeighbors.class, this::onGetNeighbors)
            .onMessage(NeighborProtocol.UpdatePosition.class, this::onUpdatePosition)
            .build();
    }

    private Behavior<NeighborProtocol.Command> onGetNeighbors(NeighborProtocol.GetNeighbors command) {
        // Logic to find neighbors within the specified radius
        P2d position = positions.get(command.boidId());
        String actual = command.boidId();
        if (position != null) {
            // Find neighbors logic here
            var neighborList = new ArrayList<BoidState>();
            for (String other : model.getBoids()) {
                if (other != actual ) {
                    P2d otherPos = other.getPos();
                    double distance = pos.distance(otherPos);
                    if (distance < model.getPerceptionRadius()) {
                        list.add(other);
                    }
                }
            }
            return list;
            // For now, just reply with an empty list
            command.replyTo().tell(new BoidProtocol.NeighborsInfo(command.boidId(), new ArrayList<>()));
        }
        return Behaviors.same();
    }

    private Behavior<NeighborProtocol.Command> onUpdatePosition(NeighborProtocol.UpdatePosition command) {
        // Update the position of the boid
        positions.put(command.boidId(), command.position());
        return Behaviors.same();
    }
}
