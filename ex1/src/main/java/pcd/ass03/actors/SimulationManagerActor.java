package pcd.ass03.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.TimerScheduler;
import pcd.ass03.model.BoidState;
import pcd.ass03.protocols.BoidProtocol;
import pcd.ass03.protocols.GUIProtocol;
import pcd.ass03.protocols.ManagerProtocol;
import pcd.ass03.protocols.NeighborProtocol;
import pcd.ass03.utils.P2d;
import pcd.ass03.utils.V2d;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class SimulationManagerActor {

    private final ActorContext<ManagerProtocol.Command> context;
    private final TimerScheduler<ManagerProtocol.Command> timers;
    private final Map<String, BoidState> currentStates = new HashMap<>();
    private final Map<String, ActorRef<BoidProtocol.Command> boidActors = new HashMap<>();
    private ActorRef<NeighborProtocol.Command> neighborhoodManager;
    private ActorRef<GUIProtocol.Command> guiActor;

    private SimulationManagerActor(ActorContext<ManagerProtocol.Command> context,
                                   TimerScheduler<ManagerProtocol.Command> timers) {
        this.context = context;
        this.timers = timers;
    }

    public static Behavior<ManagerProtocol.Command> create() {
        return Behaviors.setup(context ->
            Behaviors.withTimers(timers ->
                new SimulationManagerActor(context, timers).idle()
            )
        );
    }

    private Behavior<ManagerProtocol.Command> idle() {
        return Behaviors.receive(ManagerProtocol.Command.class)
                .onMessage(ManagerProtocol.StartSimulation.class, this::onStart)
                .build();
    }

    private Behavior<ManagerProtocol.Command> onStart(ManagerProtocol.StartSimulation cmd) {
        // Create NeighborhoodManager
        this.neighborhoodManager = context.spawn(
            NeighborhoodManagerActor.create(cmd.width(), cmd.height()),
            "neighborhood-manager"
        );

        // Create GUIActor
        this.guiActor = context.spawn(
            GUIActor.create(),
            "gui-actor"
        );

        // Create Boid actors
        for (int i = 0; i < cmd.nBoids(); i++) {
            String id = "boid-" + i;
            P2d initialPos = new P2d(-cmd.width() / 2 * Math.random() * cmd.width(),
                    -cmd.height() / 2 * cmd.height());
            ActorRef<BoidProtocol.Command> boidRef = context.spawn(
                    BoidActor.create(id, initialPos, context.getSelf()),
                    id
            );

            this.boidActors.put(id, boidRef);
            this.currentStates.put(id, new BoidState(initialPos, new V2d(0, 0), id));
        }

        // Schedule the first tick
        timers.startTimerAtFixedRate(new ManagerProtocol.Tick(), Duration.ofMillis(40));

        return running();
    }

    private Behavior<ManagerProtocol.Command> running() {
        return null;
    }


}
