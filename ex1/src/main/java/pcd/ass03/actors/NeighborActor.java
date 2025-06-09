package pcd.ass03.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import pcd.ass03.model.BoidState;
import pcd.ass03.protocols.*;
import pcd.ass03.utils.*;

import java.util.*;

public class NeighborActor {

    public static Behavior<NeighborProtocol.Command> create() {
        return Behaviors.setup(ctx -> new NeighborActor().behavior());
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
        BoidState requestingBoid = command.allBoids().stream()
                .filter(b -> b.id().equals(command.boidId()))
                .findFirst()
                .orElse(null);
        if (requestingBoid != null) {
            P2d position = requestingBoid.pos();
            for (BoidState other : command.allBoids()) {
                if (!other.id().equals(command.boidId())) {
                    double distance = position.distance(other.pos());
                    if (distance < command.radius()) {
                        neighbors.add(other);
                    }
                }
            }
        }

        command.replyTo().tell(new BoidProtocol.NeighborsInfo(command.boidId(), neighbors));
        return Behaviors.same();
    }

    private Behavior<NeighborProtocol.Command> onUpdatePosition(NeighborProtocol.UpdatePosition command) {
        // Update the position of the boid
        positions.put(command.boidId(), command.position());
        return Behaviors.same();
    }
}
