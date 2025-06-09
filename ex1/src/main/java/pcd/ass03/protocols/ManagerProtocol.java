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
     */
    record startSimulation(int nBoids,
                           double width,
                           double height) implements Command {}

    /**
     * Stop the simulation.
     */
    record stopSimulation() implements Command {}

    record tick() implements Command {}

    record boidUpdated(P2d position,
                       V2d velocity,
                       String boidId) implements Command {}

    record updateParams(double cohesion,
                        double alignment,
                        double separation) implements Command {}
}
