package pcd.ass03.protocols;

import pcd.ass03.actors.BoidsParams;
import pcd.ass03.model.*;

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
     *
     * @param tick the current tick of the simulation
     * @param boids the list of boid states
     */
    record UpdateRequest(long tick, List<BoidState> boids) implements Command {
    }

    /**
     * Command to retrieve the neighbors of a boid.
     * This command is sent to the NeighborManager to get the boids within a certain radius.
     *
     * @param boidId the unique identifier of the boid
     * @param neighbors the list of neighboring boids
     */
    record NeighborsInfo(String boidId, List<BoidState> neighbors) implements Command {
    }

    /**
     * Command to update the params for updating position of boids storm.
     *
     * @param params the new parameters for the updating logic
     */
    record UpdateParams(BoidsParams params) implements Command {
    }
}
