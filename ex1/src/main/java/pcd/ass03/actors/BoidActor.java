package pcd.ass03.actors;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import pcd.ass03.protocols.*;
import pcd.ass03.utils.*;

public class BoidActor {

    private final ActorContext<BoidProtocol.Command> context;
    private final String boidId;
    private final ActorRef<ManagerProtocol.Command> manager;

    private P2d position;
    private V2d velocity;

    public BoidActor(ActorContext<BoidProtocol.Command> context, String boidId, P2d initialPos, ActorRef<ManagerProtocol.Command> manager) {
        this.context = context;
        this.boidId = boidId;
        this.position = initialPos;
        this.manager = manager;
    }

    public static Behavior<BoidProtocol.Command> create(String boidId, P2d initialPos, ActorRef<ManagerProtocol.Command> manager) {
        // Implementation of the BoidActor behavior goes here
        return Behaviors.setup(context -> new BoidActor(context, boidId, initialPos, manager).behavior());
    }

    private Behavior<BoidProtocol.Command> behavior() {
        return Behaviors.receive(BoidProtocol.Command.class)
            .onMessage(BoidProtocol.UpdateRequest.class, this::onUpdateRequest)
            .onMessage(BoidProtocol.NeighborsInfo.class, this::onNeighborsInfo)
            .build();
    };

    private Behavior<BoidProtocol.Command> onUpdateRequest(BoidProtocol.UpdateRequest request) {
        // This could involve updating position, velocity, etc.
        return Behaviors.same();
    }

    private Behavior<BoidProtocol.Command> onNeighborsInfo(BoidProtocol.NeighborsInfo info) {
        // Handle neighbors information, possibly updating state or sending messages
        return Behaviors.same();
    }
}
