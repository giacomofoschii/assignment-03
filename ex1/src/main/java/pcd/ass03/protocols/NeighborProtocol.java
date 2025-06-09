package pcd.ass03.protocols;

import akka.actor.typed.ActorRef;
import pcd.ass03.model.BoidState;
import pcd.ass03.utils.P2d;

import java.util.List;

/**
 * Protocol for neighbor-related commands in the boids simulation.
 * This protocol defines commands for retrieving neighbors and updating boid positions.
 */
public interface NeighborProtocol {

    interface Command {}

    /**
     * Command to get the neighbors of a boid within a specified radius.
     *
     * @param boidId the unique identifier of the boid
     * @param allBoids the list of all boids in the simulation
     * @param radius the radius within which to search for neighbors
     * @param replyTo the actor reference to send the neighbors information back to
     */
    record GetNeighbors(String boidId, List<BoidState> allBoids, double radius,
                        ActorRef<BoidProtocol.NeighborsInfo> replyTo) implements Command {}

    /**
     * Command to update the position of a boid.
     *
     * @param position the new position of the boid
     * @param boidId the unique identifier of the boid
     */
    record UpdatePosition(P2d position, String boidId) implements Command {}
}
