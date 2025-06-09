package pcd.ass03.actors;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import pcd.ass03.BoidsView;
import pcd.ass03.model.BoidState;
import pcd.ass03.protocols.*;
import pcd.ass03.utils.*;

import java.time.Duration;
import java.util.*;

public class ManagerActor {

    private final ActorContext<ManagerProtocol.Command> context;
    private final TimerScheduler<ManagerProtocol.Command> timers;
    private final Map<String, BoidState> currentStates = new HashMap<>();
    private final Map<String, ActorRef<BoidProtocol.Command>> boidActors = new HashMap<>();
    private final BoidsView view;

    private ManagerActor(ActorContext<ManagerProtocol.Command> context,
                         TimerScheduler<ManagerProtocol.Command> timers,
                         BoidsView view) {
        this.context = context;
        this.timers = timers;
        this.view = view;
    }

    public static Behavior<ManagerProtocol.Command> create(BoidsView view) {
        return Behaviors.setup(context ->
            Behaviors.withTimers(timers ->
                new ManagerActor(context, timers , view).idle()
            )
        );
    }

    private Behavior<ManagerProtocol.Command> idle() {
        return Behaviors.receive(ManagerProtocol.Command.class)
                .onMessage(ManagerProtocol.StartSimulation.class, this::onStart)
                .build();
    }

    private Behavior<ManagerProtocol.Command> onStart(ManagerProtocol.StartSimulation cmd) {
        int nBoids = cmd.nBoids();
        double width = cmd.width();
        double height = cmd.height();
        BoidsParams params = new BoidsParams(width, height);

        // Create NeighborhoodManager
        ActorRef<NeighborProtocol.Command> neighborManager = context.spawn(
                NeighborActor.create(),
                "neighbor-manager"
        );

        // Create GUIActor
        ActorRef<GUIProtocol.Command> guiActor = context.spawn(
                GUIActor.create(this.view),
                "gui-actor"
        );

        // Create Boid actors
        for (int i = 0; i < nBoids; i++) {
            String id = "boid-" + i;
            P2d initialPos = new P2d(-width / 2 * Math.random() * width,
                    -height / 2 * height);
            ActorRef<BoidProtocol.Command> boidRef = context.spawn(
                    BoidActor.create(id, initialPos, context.getSelf(), neighborManager, params),
                    id
            );

            this.boidActors.put(id, boidRef);
            this.currentStates.put(id, new BoidState(initialPos, new V2d(0, 0), id));
        }

        // Schedule the first tick
        timers.startTimerAtFixedRate(new ManagerProtocol.Tick(), Duration.ofMillis(40));

        return running();
    }

    //TODO: Implement the running behavior
    private Behavior<ManagerProtocol.Command> running() {
        return null;
    }


}
