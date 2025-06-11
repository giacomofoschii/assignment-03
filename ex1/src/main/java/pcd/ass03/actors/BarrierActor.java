package pcd.ass03.actors;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import pcd.ass03.protocols.*;

import java.util.*;

public class BarrierActor {
    private final int nBoids;
    private final ActorRef<ManagerProtocol.Command> manager;
    private final Set<String> completedBoids = new HashSet<>();

    public BarrierActor(int nBoids,
                        ActorRef<ManagerProtocol.Command> manager) {
        this.nBoids = nBoids;
        this.manager = manager;
    }

    public static Behavior<BarrierProtocol.Command> create(int nBoids, ActorRef<ManagerProtocol.Command> manager) {
        return Behaviors.setup(context -> new BarrierActor(nBoids, manager).behavior());
    }

    private Behavior<BarrierProtocol.Command> behavior() {
        return Behaviors.receive(BarrierProtocol.Command.class)
                .onMessage(BarrierProtocol.StartPhase.class, this::onStartPhase)
                .onMessage(BarrierProtocol.BoidCompleted.class, this::onBoidCompleted)
                .build();
    }

    private Behavior<BarrierProtocol.Command> onStartPhase(BarrierProtocol.StartPhase cmd) {
        completedBoids.clear();
        return Behaviors.same();
    }

    private Behavior<BarrierProtocol.Command> onBoidCompleted(BarrierProtocol.BoidCompleted cmd) {
        completedBoids.add(cmd.boidId());

        if(completedBoids.size() >= nBoids) {
            manager.tell(new ManagerProtocol.UpdateCompleted(cmd.tick()));
            completedBoids.clear();
        }

        return Behaviors.same();
    }
}
