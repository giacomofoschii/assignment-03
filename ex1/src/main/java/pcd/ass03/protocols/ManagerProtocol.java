package pcd.ass03.protocols;

import pcd.ass03.utils.P2d;
import pcd.ass03.utils.V2d;

/**
 * Protocol for Manager actor that control simulation.
 */
public interface ManagerProtocol {

    interface Command {}

    /**
     * Start the simulation.
     *
     * @param nBoids the number of boids to simulate
     * @param width the width of the simulation area
     * @param height the height of the simulation area
     */
    record StartSimulation(int nBoids, double width, double height) implements Command {}

    /**
     * Stop the simulation.
     */
    record StopSimulation() implements Command {}

    /**
     * Tick command to update the simulation state.
     */
    record Tick() implements Command {}

    /**
     * Command to update the position and velocity of a boid.
     *
     * @param position the new position of the boid
     * @param velocity the new velocity of the boid
     * @param boidId the unique identifier of the boid
     */
    record BoidUpdated(P2d position, V2d velocity, String boidId) implements Command {}

    /**
     * Command to update the parameters of the simulation, setted by user from GUI.
     *
     * @param cohesion cohesion factor
     * @param alignment alignment factor
     * @param separation separation factor
     */
    record UpdateParams(double cohesion, double alignment, double separation) implements Command {}
}
