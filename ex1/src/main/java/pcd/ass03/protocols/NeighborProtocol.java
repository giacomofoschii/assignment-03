package pcd.ass03.protocols;

import akka.actor.typed.ActorRef;
import pcd.ass03.utils.P2d;
import pcd.ass03.utils.V2d;

public interface NeighborProtocol {

    interface Command {}

    /**
     * Command to get the neighbors of a boid within a specified radius.
     *
     * @param boidId the unique identifier of the boid
     * @param position the current position of the boid
     * @param radius the radius within which to search for neighbors
     * @param replyTo the actor reference to send the neighbors information back to
     */
    record GetNeighbors(String boidId,
                        P2d position,
                        double radius,
                        ActorRef<BoidProtocol.NeighborsInfo> replyTo) implements Command {}

    /**
     * Command to update the position of a boid.
     *
     * @param position the new position of the boid
     * @param boidId the unique identifier of the boid
     */
    record UpdatePosition(P2d position,
                       String boidId) implements Command {}
}
