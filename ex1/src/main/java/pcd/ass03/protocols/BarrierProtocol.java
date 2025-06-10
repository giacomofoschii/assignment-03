package pcd.ass03.protocols;

/**
 * Protocol for managing barriers in the Boids simulation.
 * Defines commands for starting a phase and notifying when a boid has completed its work.
 */
public interface BarrierProtocol {

    /**
     * Command interface for Barrier actors.
     * All commands sent to Barrier actors must implement this interface.
     */
    interface Command {}

    /**
     * Command to start a new phase in the simulation.
     * This command is sent to the Barrier actor to initiate a new phase based on the current tick.
     *
     * @param tick the current tick of the simulation
     */
    record StartPhase(long tick) implements Command {}

    /**
     * Command to notify that a boid has completed its work for the current tick.
     * This command is sent to the Barrier actor when a boid finishes its processing.
     *
     * @param boidId the unique identifier of the boid that has completed
     * @param tick the current tick of the simulation
     */
    record BoidCompleted(String boidId, long tick) implements Command {}

}
