package pcd.ass03.actors;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import pcd.ass03.model.*;
import pcd.ass03.protocols.*;
import pcd.ass03.utils.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ManagerActor {
    private static final int FPS = 60;
    private static final int TICK_NUMBER = 40;
    private static final AtomicInteger VERSION = new AtomicInteger(0);

    private final ActorContext<ManagerProtocol.Command> context;
    private ActorRef<GUIProtocol.Command> guiActor;
    private final TimerScheduler<ManagerProtocol.Command> timers;
    private final Map<String, BoidState> currentStates = new HashMap<>();
    private final Map<String, ActorRef<BoidProtocol.Command>> boidActors = new HashMap<>();
    private int completedBoids;
    private final BoidsParams boidsParams;
    private long currentTick;
    private long lastFrameTime;

    private ManagerActor(ActorContext<ManagerProtocol.Command> context,
                         TimerScheduler<ManagerProtocol.Command> timers) {
        this.context = context;
        this.timers = timers;
        this.boidsParams = new BoidsParams(800,800);
        this.currentTick = 0;
        this.lastFrameTime = 0;
        this.completedBoids = 0;
    }

    public static Behavior<ManagerProtocol.Command> create() {
        return Behaviors.setup(context ->
                Behaviors.withTimers(timers ->
                    Behaviors.supervise(new ManagerActor(context, timers).idle())
                            .onFailure(SupervisorStrategy.restart())
        ));
    }

    private Behavior<ManagerProtocol.Command> idle() {
        guiActor = context.spawn(
                Behaviors.supervise(GUIActor.create(boidsParams, this.context.getSelf()))
                        .onFailure(SupervisorStrategy.restart()), "gui-actor" + VERSION.getAndIncrement()
        );

        return Behaviors.receive(ManagerProtocol.Command.class)
                .onMessage(ManagerProtocol.StartSimulation.class, this::onStart)
                .onMessage(ManagerProtocol.BoidUpdated.class, msg -> Behaviors.same()) // Ignore residual updates
                .onMessage(ManagerProtocol.UpdateCompleted.class, msg -> Behaviors.same()) // Ignore residual completions
                .onMessage(ManagerProtocol.Tick.class, msg -> Behaviors.same()) // Ignore residual ticks
                .build();
    }

    private Behavior<ManagerProtocol.Command> onStart(ManagerProtocol.StartSimulation cmd) {
        int nBoids = cmd.nBoids();
        double width = cmd.width();
        double height = cmd.height();

        // Create Boid actors
        for (int i = 0; i < nBoids; i++) {
            String id = "boid-" + i;
            P2d initialPos = new P2d(
                    (Math.random() - 0.5) * width,
                    (Math.random() - 0.5) * height);
            V2d initialVel = new V2d(
                    (Math.random() - 0.5) * 2,
                    (Math.random() - 0.5) * 2);

            ActorRef<BoidProtocol.Command> boidRef = context.spawn(
                    Behaviors.supervise(BoidActor.create(id, initialPos, initialVel, boidsParams,
                                    this.context.getSelf()))
                            .onFailure(SupervisorStrategy.restartWithBackoff(
                                    Duration.ofMillis(100), Duration.ofSeconds(1), 0.2f)
                                    .withResetBackoffAfter(Duration.ofSeconds(10))),
                    id + "-" + System.currentTimeMillis());

            this.boidActors.put(id, boidRef);
            this.currentStates.put(id, new BoidState(initialPos, initialVel, id));
        }

        guiActor.tell(new GUIProtocol.UpdateWeights(
                boidsParams.getSeparationWeight(),
                boidsParams.getAlignmentWeight(),
                boidsParams.getCohesionWeight()
        ));

        guiActor.tell(new GUIProtocol.RenderFrame(
                this.currentStates.values().stream().toList(),
                new SimulationMetrics(cmd.nBoids(), FPS, TICK_NUMBER)
        ));

        guiActor.tell(new GUIProtocol.UpdateStatus(GUIProtocol.SimulationStatus.RUNNING));

        // Schedule the first tick
        timers.startTimerAtFixedRate(new ManagerProtocol.Tick(), Duration.ofMillis(TICK_NUMBER));

        return running();
    }

    private Behavior<ManagerProtocol.Command> running() {
        return Behaviors.receive(ManagerProtocol.Command.class)
                .onMessage(ManagerProtocol.Tick.class, this::onTick)
                .onMessage(ManagerProtocol.UpdateCompleted.class, this::onUpdateCompleted)
                .onMessage(ManagerProtocol.BoidUpdated.class, this::onBoidUpdated)
                .onMessage(ManagerProtocol.UpdateParams.class, this::onUpdateParams)
                .onMessage(ManagerProtocol.PauseSimulation.class, this::onPause)
                .onMessage(ManagerProtocol.ResumeSimulation.class, this::onResume)
                .onMessage(ManagerProtocol.StopSimulation.class, this::onStop)
                .build();
    }

    private Behavior<ManagerProtocol.Command> onResume(ManagerProtocol.ResumeSimulation resumeSimulation) {
        completedBoids = 0;

        // Restart the timer
        timers.startTimerAtFixedRate(new ManagerProtocol.Tick(), Duration.ofMillis(TICK_NUMBER));

        boidActors.values().forEach(boidActor ->
                boidActor.tell(new BoidProtocol.UpdateRequest(currentTick,
                        currentStates.values().stream().toList())));

        if (guiActor != null) {
            guiActor.tell(new GUIProtocol.ConfirmResume());
        }

        return running();
    }

    private Behavior<ManagerProtocol.Command> onPause(ManagerProtocol.PauseSimulation pauseSimulation) {
        // Stop the timer
        timers.cancel(new ManagerProtocol.Tick());

        boidActors.values().forEach(boidActor ->
                boidActor.tell(new BoidProtocol.WaitUpdateRequest(currentTick)));

        if(guiActor != null) {
            guiActor.tell(new GUIProtocol.ConfirmPause());
        }

        return paused();
    }

    private Behavior<ManagerProtocol.Command> onStop(ManagerProtocol.StopSimulation stopSimulation) {
        timers.cancelAll();

        if (guiActor != null) {
            guiActor.tell(new GUIProtocol.ConfirmStop());
        }

        boidActors.values().forEach(this.context::stop);

        context.scheduleOnce(Duration.ofMillis(100), context.getSelf(), new DelayedStopGUI());

        return stopping();
    }

    private Behavior<ManagerProtocol.Command> stopping() {
        return Behaviors.receive(ManagerProtocol.Command.class)
                .onMessage(DelayedStopGUI.class, msg -> {

                    if (guiActor != null) {
                        this.context.stop(guiActor);
                    }

                    return create();
                })
                .onMessage(ManagerProtocol.BoidUpdated.class, msg -> Behaviors.same())
                .onMessage(ManagerProtocol.UpdateCompleted.class, msg -> Behaviors.same())
                .onMessage(ManagerProtocol.Tick.class, msg -> Behaviors.same())
                .build();
    }

    private Behavior<ManagerProtocol.Command> onUpdateCompleted(ManagerProtocol.UpdateCompleted cmd) {
        long currentMills = System.currentTimeMillis();
        int fps = 0;
        if (lastFrameTime != 0) {
            long delta = currentMills - lastFrameTime;
            if (delta > 0) {
                fps = (int) (1000.0 / delta);
                if (fps > FPS) {
                    fps = FPS;
                }
            }
        }
        lastFrameTime = currentMills;

        if (guiActor != null) {
            guiActor.tell(new GUIProtocol.RenderFrame(
                    currentStates.values().stream().toList(),
                    new SimulationMetrics(boidActors.size(), fps, currentMills - cmd.tick())
            ));
        }

        completedBoids = 0;

        return Behaviors.same();
    }

    private Behavior<ManagerProtocol.Command> onBoidUpdated(ManagerProtocol.BoidUpdated boidUpdated) {
        String boidId = boidUpdated.boidId();
        completedBoids ++;
        currentStates.put(boidId, new BoidState(boidUpdated.position(), boidUpdated.velocity(), boidId));
        if(completedBoids >= boidActors.size()) {
            this.context.getSelf().tell(new ManagerProtocol.UpdateCompleted(currentTick));
        }

        return Behaviors.same();
    }

    private Behavior<ManagerProtocol.Command> onTick(ManagerProtocol.Tick tick) {
        currentTick++;

        if(completedBoids == 0) {
            for (ActorRef<BoidProtocol.Command> boidActor : boidActors.values()) {
                boidActor.tell(new BoidProtocol.UpdateRequest(currentTick,
                        currentStates.values().stream().toList()));
            }
        }

        return Behaviors.same();
    }

    private Behavior<ManagerProtocol.Command> onUpdateParams(ManagerProtocol.UpdateParams updateParams) {
        boidsParams.setAlignmentWeight(updateParams.alignment());
        boidsParams.setCohesionWeight(updateParams.cohesion());
        boidsParams.setSeparationWeight(updateParams.separation());

        for (ActorRef<BoidProtocol.Command> boidActor : boidActors.values()) {
            boidActor.tell(new BoidProtocol.UpdateParams(boidsParams));
        }

        if (guiActor != null) {
            guiActor.tell(new GUIProtocol.UpdateWeights(
                    boidsParams.getSeparationWeight(),
                    boidsParams.getAlignmentWeight(),
                    boidsParams.getCohesionWeight()
            ));
            guiActor.tell(new GUIProtocol.ConfirmParamsUpdate());
        }

        return Behaviors.same();
    }

    private Behavior<ManagerProtocol.Command> paused() {
        return Behaviors.receive(ManagerProtocol.Command.class)
                .onMessage(ManagerProtocol.ResumeSimulation.class, this::onResume)
                .onMessage(ManagerProtocol.StopSimulation.class, this::onStop)
                .onMessage(ManagerProtocol.UpdateParams.class, this::onUpdateParams)
                .onMessage(ManagerProtocol.BoidUpdated.class, msg -> Behaviors.same())
                .onMessage(ManagerProtocol.UpdateCompleted.class, msg -> Behaviors.same())
                .onMessage(ManagerProtocol.Tick.class, msg -> Behaviors.same())
                .build();
    }

    // Internal command to delay the stop of the GUI actor
    private static class DelayedStopGUI implements ManagerProtocol.Command {}
}
