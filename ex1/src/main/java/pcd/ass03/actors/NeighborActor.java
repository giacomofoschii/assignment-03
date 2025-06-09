package pcd.ass03.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import pcd.ass03.model.BoidState;
import pcd.ass03.protocols.*;
import pcd.ass03.utils.*;

import java.util.*;

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
        List<BoidState> neighbors = new ArrayList<>();
        P2d position = positions.get(command.boidId());
        List<BoidState> boids = command.allBoids();
        if (position != null) {
            for (BoidState other : boids) {
                if (!Objects.equals(other.id(), command.boidId())) {
                    P2d otherPos = other.pos();
                    double distance = position.distance(otherPos);
                    if (distance < command.radius()) {
                        neighbors.add(other);
                    }
                }
            }
            // For now, just reply with an empty list
            command.replyTo().tell(new BoidProtocol.NeighborsInfo(command.boidId(), neighbors));
        }
        return Behaviors.same();
    }

    private Behavior<NeighborProtocol.Command> onUpdatePosition(NeighborProtocol.UpdatePosition command) {
        // Update the position of the boid
        positions.put(command.boidId(), command.position());
        return Behaviors.same();
    }
}
