package pcd.ass03.actors;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import pcd.ass03.model.BoidState;
import pcd.ass03.protocols.*;
import pcd.ass03.utils.*;

import java.time.Duration;
import java.util.List;

public class BoidActor {
    private final ActorContext<BoidProtocol.Command> context;
    private final String boidId;
    private final ActorRef<NeighborProtocol.Command> neighborManager;
    private final ActorRef<ManagerProtocol.Command> managerActor;

    private P2d position;
    private V2d velocity;
    private final BoidsParams params;

    public BoidActor(ActorContext<BoidProtocol.Command> context, String boidId, P2d initialPos,
                     ActorRef<NeighborProtocol.Command> neighborManager, BoidsParams params,
                     ActorRef<ManagerProtocol.Command> managerActor) {
        this.context = context;
        this.boidId = boidId;
        this.position = initialPos;
        this.neighborManager = neighborManager;
        this.params = params;
        this.managerActor = managerActor;
    }

    public static Behavior<BoidProtocol.Command> create(String boidId, P2d initialPos,
                                                        ActorRef<NeighborProtocol.Command> neighborManager,
                                                        BoidsParams params,
                                                        ActorRef<ManagerProtocol.Command> managerActor) {
        // Implementation of the BoidActor behavior goes here
        return Behaviors.setup(context -> new BoidActor(context, boidId, initialPos,
                neighborManager, params, managerActor).behavior());
    }

    private Behavior<BoidProtocol.Command> behavior() {
        return Behaviors.receive(BoidProtocol.Command.class)
            .onMessage(BoidProtocol.UpdateRequest.class, this::onUpdateRequest)
            .onMessage(BoidProtocol.NeighborsInfo.class, this::onNeighborsInfo)
            .build();
    }

    private Behavior<BoidProtocol.Command> onUpdateRequest(BoidProtocol.UpdateRequest request) {
        context.ask(BoidProtocol.NeighborsInfo.class, neighborManager, Duration.ofMillis(100),
                replyTo -> new NeighborProtocol.GetNeighbors(boidId, request.boids(),
                                                                            params.getPerceptionRadius(), replyTo),
                (response, throwable) -> {
                    if (response != null) {
                        return response;
                    } else {
                        // Handle the case where the response is null or an error occurred
                        context.getLog().error("Failed to get neighbors for boid {}", boidId);
                        return new BoidProtocol.NeighborsInfo(boidId, List.of());
                    }
                });

        return Behaviors.same();
    }

    private Behavior<BoidProtocol.Command> onNeighborsInfo(BoidProtocol.NeighborsInfo info) {
        List<BoidState> nearbyBoids = info.neighbors();

        V2d separation = calculateSeparation(nearbyBoids);
        V2d alignment = calculateAlignment(nearbyBoids);
        V2d cohesion = calculateCohesion(nearbyBoids);

        velocity = velocity.sum(alignment.mul(params.getAlignmentWeight()))
                .sum(separation.mul(params.getSeparationWeight()))
                .sum(cohesion.mul(params.getCohesionWeight()));

        /* Limit speed to MAX_SPEED */
        double speed = velocity.abs();

        if (speed > params.getMaxSpeed()) {
            velocity = velocity.getNormalized().mul(params.getMaxSpeed());
        }

        /* Update position */
        position = position.sum(velocity);

        /* environment wrap-around */
        if (position.x() < params.getMinX()) position = position.sum(new V2d(params.getWidth(), 0));
        if (position.x() >= params.getMaxX()) position = position.sum(new V2d(-params.getWidth(), 0));
        if (position.y() < params.getMinY()) position = position.sum(new V2d(0, params.getHeight()));
        if (position.y() >= params.getMaxY()) position = position.sum(new V2d(0, -params.getHeight()));

        managerActor.tell(new ManagerProtocol.BoidUpdated(position, velocity, boidId));

        return Behaviors.same();
    }

    private V2d calculateAlignment(List<BoidState> nearbyBoids) {
        double avgVx = 0;
        double avgVy = 0;
        if (!nearbyBoids.isEmpty()) {
            for (BoidState other : nearbyBoids) {
                V2d otherVel = other.vel();
                avgVx += otherVel.x();
                avgVy += otherVel.y();
            }
            avgVx /= nearbyBoids.size();
            avgVy /= nearbyBoids.size();
            return new V2d(avgVx - velocity.x(), avgVy - velocity.y()).getNormalized();
        } else {
            return new V2d(0, 0);
        }
    }

    private V2d calculateCohesion(List<BoidState> nearbyBoids) {
        double centerX = 0;
        double centerY = 0;
        if (!nearbyBoids.isEmpty()) {
            for (BoidState other: nearbyBoids) {
                P2d otherPos = other.pos();
                centerX += otherPos.x();
                centerY += otherPos.y();
            }
            centerX /= nearbyBoids.size();
            centerY /= nearbyBoids.size();
            return new V2d(centerX - position.x(), centerY - position.y()).getNormalized();
        } else {
            return new V2d(0, 0);
        }
    }

    private V2d calculateSeparation(List<BoidState> nearbyBoids) {
        double dx = 0;
        double dy = 0;
        int count = 0;
        for (BoidState other: nearbyBoids) {
            P2d otherPos = other.pos();
            double distance = position.distance(otherPos);
            if (distance < params.getAvoidRadius()) {
                dx += position.x() - otherPos.x();
                dy += position.y() - otherPos.y();
                count++;
            }
        }
        if (count > 0) {
            dx /= count;
            dy /= count;
            return new V2d(dx, dy).getNormalized();
        } else {
            return new V2d(0, 0);
        }
    }
}
