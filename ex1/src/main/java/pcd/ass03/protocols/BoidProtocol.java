package pcd.ass03.protocols;

import akka.actor.typed.ActorRef;
import pcd.ass03.model.BoidState;
import pcd.ass03.model.StateResponse;

import java.util.List;

/**
 * Protocol for Boid actors in the Boids simulation.
 * Defines commands for updating boid states, retrieving neighbors, and getting the state of a boid.
 */
public interface BoidProtocol {

    /**
     * Command interface for Boid actors.
     * All commands sent to Boid actors must implement this interface.
     */
    interface Command {}

    /**
     * Response interface for Boid actors.
     * All responses from Boid actors must implement this interface.
     */
    record UpdateRequest(long tick) implements Command {
        public UpdateRequest {
            if (tick < 0) {
                throw new IllegalArgumentException("Tick must be non-negative");
            }
        }
    }

    record NeighborsInfo(List<BoidState> neighbors) implements Command {
        public NeighborsInfo {
            if (neighbors == null) {
                throw new IllegalArgumentException("Neighbors list cannot be null");
            }
        }
    }

    record GetState(ActorRef<StateResponse> replyTo) implements Command {
        public GetState {
            if (replyTo == null) {
                throw new IllegalArgumentException("ReplyTo actor reference cannot be null");
            }
        }
    }
}
