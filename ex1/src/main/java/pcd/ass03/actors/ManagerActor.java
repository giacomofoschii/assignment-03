package pcd.ass03.actors;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import pcd.ass03.model.BoidState;
import pcd.ass03.model.SimulationMetrics;
import pcd.ass03.protocols.*;
import pcd.ass03.utils.*;

import java.time.Duration;
import java.util.*;

public class ManagerActor {
    private static final int FPS = 25;
    private static final int TICK_NUMBER = 40;

    private final ActorContext<ManagerProtocol.Command> context;
    private ActorRef<GUIProtocol.Command> guiActor;
    private final TimerScheduler<ManagerProtocol.Command> timers;
    private final Map<String, BoidState> currentStates = new HashMap<>();
    private final Map<String, ActorRef<BoidProtocol.Command>> boidActors = new HashMap<>();
    private BoidsParams boidsParams;

    private ManagerActor(ActorContext<ManagerProtocol.Command> context,
                         TimerScheduler<ManagerProtocol.Command> timers) {
        this.context = context;
        this.timers = timers;
        this.boidsParams = new BoidsParams(800, 800); // Default parameters
    }

    public static Behavior<ManagerProtocol.Command> create() {
        return Behaviors.setup(context ->
            Behaviors.withTimers(timers ->
                new ManagerActor(context, timers ).idle()
            )
        );
    }

    private Behavior<ManagerProtocol.Command> idle() {
        // Create GUIActor
        //Uncomment if you need guiActor variable
        guiActor = context.spawn(
                GUIActor.create(boidsParams, this.context.getSelf()),
                "gui-actor"
        );

        return Behaviors.receive(ManagerProtocol.Command.class)
                .onMessage(ManagerProtocol.StartSimulation.class, this::onStart)
                .build();
    }

    private Behavior<ManagerProtocol.Command> onStart(ManagerProtocol.StartSimulation cmd) {
        int nBoids = cmd.nBoids();
        double width = cmd.width();
        double height = cmd.height();

        // Create NeighborhoodManager
        ActorRef<NeighborProtocol.Command> neighborManager = context.spawn(
                NeighborActor.create(),
                "neighbor-manager"
        );

        // Create Boid actors
        for (int i = 0; i < nBoids; i++) {
            String id = "boid-" + i;
            P2d initialPos = new P2d(Math.random() * width, Math.random() * height);
            V2d initialVel = new V2d(
                    (Math.random() - 0.5) * 2,
                    (Math.random() - 0.5) * 2
            );
            ActorRef<BoidProtocol.Command> boidRef = context.spawn(
                    BoidActor.create(id, initialPos, neighborManager, boidsParams, this.context.getSelf()),
                    id
            );

            this.boidActors.put(id, boidRef);
            this.currentStates.put(id, new BoidState(initialPos, initialVel, id));
        }

        guiActor.tell(new GUIProtocol.RenderFrame(this.currentStates.values().stream().toList(),
                new SimulationMetrics(cmd.nBoids(), FPS, TICK_NUMBER)));

        // Schedule the first tick
        timers.startTimerAtFixedRate(new ManagerProtocol.Tick(), Duration.ofMillis(TICK_NUMBER));

        return running();
    }

    private Behavior<ManagerProtocol.Command> running() {
        return Behaviors.receive(ManagerProtocol.Command.class)
                .onMessage(ManagerProtocol.Tick.class, this::onTick)
                .onMessage(ManagerProtocol.BoidUpdated.class, this::onBoidUpdated)
                .onMessage(ManagerProtocol.UpdateParams.class, this::onUpdateParams)
                .onMessage(ManagerProtocol.PauseSimulation.class, this::onPause)
                .onMessage(ManagerProtocol.ResumeSimulation.class, this::onResume)
                .onMessage(ManagerProtocol.StopSimulation.class, this::onStop)
                .build();
    }

    private Behavior<ManagerProtocol.Command> onResume(ManagerProtocol.ResumeSimulation resumeSimulation) {
        // Restart the timer
        timers.startTimerAtFixedRate(new ManagerProtocol.Tick(), Duration.ofMillis(TICK_NUMBER));
        return running();
    }

    private Behavior<ManagerProtocol.Command> onPause(ManagerProtocol.PauseSimulation pauseSimulation) {
        // Stop the timer
        timers.cancel(new ManagerProtocol.Tick());
        return paused();
    }

    private Behavior<ManagerProtocol.Command> onStop(ManagerProtocol.StopSimulation stopSimulation) {
        timers.cancelAll();
        return Behaviors.stopped();
    }

    private Behavior<ManagerProtocol.Command> onBoidUpdated(ManagerProtocol.BoidUpdated boidUpdated) {
        String boidId = boidUpdated.boidId();
        currentStates.put(boidId, new BoidState(boidUpdated.position(), boidUpdated.velocity(), boidId));

        return Behaviors.same();
    }

    private Behavior<ManagerProtocol.Command> onTick(ManagerProtocol.Tick tick) {
        List<BoidState> states = new ArrayList<>(currentStates.values());
        for(ActorRef<BoidProtocol.Command> boidActor : boidActors.values()) {
            boidActor.tell(new BoidProtocol.UpdateRequest(System.currentTimeMillis(), states));
        }

        guiActor.tell(new GUIProtocol.RenderFrame(states,
                new SimulationMetrics(boidActors.size(), FPS, System.currentTimeMillis())));

        return Behaviors.same();
    }

    private Behavior<ManagerProtocol.Command> onUpdateParams(ManagerProtocol.UpdateParams updateParams) {
        boidsParams.setAlignmentWeight(updateParams.alignment());
        boidsParams.setCohesionWeight(updateParams.cohesion());
        boidsParams.setSeparationWeight(updateParams.separation());

        for (ActorRef<BoidProtocol.Command> boidActor : boidActors.values()) {
            boidActor.tell(new BoidProtocol.UpdateParams(boidsParams));
        }

        return Behaviors.same();
    }

    private Behavior<ManagerProtocol.Command> paused() {
        return Behaviors.receive(ManagerProtocol.Command.class)
                .onMessage(ManagerProtocol.ResumeSimulation.class, this::onResume)
                .onMessage(ManagerProtocol.StopSimulation.class, this::onStop)
                .build();
    }
}
