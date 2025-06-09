package pcd.ass03.protocols;

import akka.actor.typed.ActorRef;
import pcd.ass03.utils.P2d;
import pcd.ass03.utils.V2d;

import java.util.List;
import java.util.Objects;

/**
 * Protocol messages for Boid actors.
 */
public interface BoidProtocol {

    // Base interface for all messages that a BoidActor can receive, used to have type safety.
    interface Command {}

    // Info about a Boid used for neighbor boids communication
    record BoidInfo(String id, P2d pos, V2d vel) {
        public BoidInfo {
            Objects.requireNonNull(id, "id cannot be null");
            Objects.requireNonNull(pos, "position cannot be null");
            Objects.requireNonNull(vel, "velocity cannot be null");
        }

        public double distanceTo(BoidInfo other) {
            return pos.distance(other.pos);
        }
    }

    // Simulation params can be updated at runtime
    record SimulationParams(
            double separationWeight,
            double alignmentWeight,
            double cohesionWeight,
            double maxSpeed,
            double perceptionRadius,
            double avoidanceRadius,
            double envWidth,
            double envHeight
    ) {
        public SimulationParams {
            if (separationWeight < 0 || alignmentWeight < 0 || cohesionWeight < 0) {
                throw new IllegalArgumentException("Weights must be non-negative");
            }
            if (maxSpeed <= 0) {
                throw new IllegalArgumentException("maxSpeed must be positive");
            }
            if (perceptionRadius <= 0) {
                throw new IllegalArgumentException("perceptionRadius must be positive");
            }
            if (avoidanceRadius <= 0 || avoidanceRadius > perceptionRadius) {
                throw new IllegalArgumentException("avoidanceRadius must be positive and <= perceptionRadius");
            }
            if (envWidth <= 0 || envHeight <= 0) {
                throw new IllegalArgumentException("Environment dimensions must be positive");
            }
        }

        public static SimulationParams createDefault() {
            return new SimulationParams(
                    1.0,
                    1.0,
                    1.0,
                    4.0,
                    50.0,
                    20.0,
                    1000.0,
                    1000.0
            );
        }
    }

    // Initialize the Boid
    record Initialize(String id, P2d initialPos, V2d initialVel) implements Command {
        public Initialize {
            Objects.requireNonNull(id, "id cannot be null");
            Objects.requireNonNull(initialPos, "initial position cannot be null");
            Objects.requireNonNull(initialVel, "initial velocity cannot be null");
        }
    }

    // Request to update position for a specific simulation tick
    record UpdatePos(long tick) implements Command {
        public UpdatePos {
            if (tick < 0) {
                throw new IllegalArgumentException("Tick must be non-negative");
            }
        }
    }

    // Response with neighboring boids info
    record Neighbors(List<BoidInfo> nearbyBoids, long tick) implements Command {
        public Neighbors {
            Objects.requireNonNull(nearbyBoids, "nearbyBoids cannot be null");
            nearbyBoids = List.copyOf(nearbyBoids);
            if (tick < 0) {
                throw new IllegalArgumentException("Tick must be non-negative");
            }
        }
    }

    // Update simulation param
    record UpdateParams(SimulationParams params) implements Command {
        public UpdateParams {
            Objects.requireNonNull(params, "parameters cannot be null");
        }
    }

    // Request current state
    record GetState(ActorRef<StateResponse> replyTo) implements Command {
        public GetState {
            Objects.requireNonNull(replyTo, "replyTo cannot be null");
        }
    }

    // Response with the current state of the Boid
    // USED ONLY FOR RESPONSES TO WHO REQUESTS THE STATE
    record StateResponse(BoidInfo state) {
        public StateResponse {
            Objects.requireNonNull(state, "state cannot be null");
        }
    }
}
