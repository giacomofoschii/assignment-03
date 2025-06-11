package pcd.ass03.actors;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import pcd.ass03.model.BoidState;
import pcd.ass03.protocols.*;
import pcd.ass03.utils.*;

import java.util.List;

public class BoidActor {
    private final String boidId;
    private final ActorRef<ManagerProtocol.Command> managerActor;
    private final ActorRef<BarrierProtocol.Command> barrierActor;
    private P2d position;
    private V2d velocity;
    private BoidsParams params;

    public BoidActor(String boidId, P2d initialPos, V2d initialVel, BoidsParams params,
                     ActorRef<ManagerProtocol.Command> managerActor, ActorRef<BarrierProtocol.Command> barrierActor) {
        this.boidId = boidId;
        this.position = initialPos;
        this.velocity = initialVel;
        this.params = params;
        this.managerActor = managerActor;
        this.barrierActor = barrierActor;
    }

    public static Behavior<BoidProtocol.Command> create(String boidId, P2d initialPos, V2d initialVel,
                                                        BoidsParams params,
                                                        ActorRef<ManagerProtocol.Command> managerActor,
                                                        ActorRef<BarrierProtocol.Command> barrierActor) {
        // Implementation of the BoidActor behavior goes here
        return Behaviors.setup(context -> new BoidActor(boidId, initialPos, initialVel,
                                                        params, managerActor, barrierActor).behavior());
    }

    private Behavior<BoidProtocol.Command> behavior() {
        return Behaviors.receive(BoidProtocol.Command.class)
            .onMessage(BoidProtocol.UpdateRequest.class, this::onUpdateRequest)
            .onMessage(BoidProtocol.UpdateParams.class, this::onUpdateParams)
            .build();
    }

    private Behavior<BoidProtocol.Command> onUpdateParams(BoidProtocol.UpdateParams params) {
        this.params = params.params();
        return Behaviors.same();
    }

    private Behavior<BoidProtocol.Command> onUpdateRequest(BoidProtocol.UpdateRequest request) {
        List<BoidState> nearbyBoids = findNearby(request.boids());

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
        barrierActor.tell(new BarrierProtocol.BoidCompleted(boidId, request.tick()));

        return Behaviors.same();
    }

    private List<BoidState> findNearby(List<BoidState> boids) {
        return boids.stream()
                .filter(other -> !other.id().equals(boidId)
                        && position.distance(other.pos()) <= params.getPerceptionRadius())
                .toList();
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
