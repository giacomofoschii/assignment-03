package pcd.ass03.protocols;

import akka.actor.typed.ActorRef;
import pcd.ass03.utils.P2d;
import pcd.ass03.utils.V2d;

/**
 * Protocol for Manager actor that control simulation.
 * This protocol defines the commands that can be sent to the Manager actor
 */
public interface ManagerProtocol {

    interface Command {}
    interface Response {}

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
     * Pause the simulation.
     * This command is used to pause the simulation without stopping it completely.
     * It allows the simulation to be resumed later.
     */
    record PauseSimulation() implements Command {}

    /**
     * Resume the simulation.
     */
    record ResumeSimulation() implements Command {}


    /**
     * Command to update the parameters of the simulation, setted by user from GUI.
     *
     * @param cohesion cohesion factor
     * @param alignment alignment factor
     * @param separation separation factor
     */
    record UpdateParams(double cohesion, double alignment, double separation) implements Command {}


    /**
     * Response messages sent back to the GUI or other actors after processing commands.
     *
     * @param success indicates if the operation was successful
     * @param message a message providing additional information about the operation
     */
    record StartSimulationResponse(boolean success, String message) implements Response {}

    /**
     * Response for stopping the simulation.
     * This response is sent back to the GUI after the simulation has been stopped.
     *
     * @param success indicates if the stop operation was successful
     */
    record StopSimulationResponse(boolean success) implements Response {}

    /**
     * Response for pausing the simulation.
     * This response is sent back to the GUI after the simulation has been paused.
     *
     * @param success indicates if the pause operation was successful
     */
    record PauseSimulationResponse(boolean success) implements Response {}

    /**
     * Response for resuming the simulation.
     * This response is sent back to the GUI after the simulation has been resumed.
     *
     * @param success indicates if the resume operation was successful
     */
    record ResumeSimulationResponse(boolean success) implements Response {}

    /**
     * Response for updating the parameters of the simulation.
     * This response is sent back to the GUI after the parameters have been updated.
     *
     * @param success indicates if the update was successful
     * @param cohesion the new cohesion factor
     * @param alignment the new alignment factor
     * @param separation the new separation factor
     */
    record UpdateParamsResponse(boolean success, double cohesion, double alignment, double separation) implements Response {}

    /**
     * Tick command to update the simulation state.
     */
    record Tick() implements Command {}

    /**
     * Command to notify that the update for the current tick is completed.
     * This command is sent to the Manager actor when all boids have completed their updates for the current tick.
     *
     * @param tick the current tick of the simulation
     */
    record UpdateCompleted(long tick) implements Command {}

    /**
     * Command to update the position and velocity of a boid.
     *
     * @param position the new position of the boid
     * @param velocity the new velocity of the boid
     * @param boidId the unique identifier of the boid
     */
    record BoidUpdated(P2d position, V2d velocity, String boidId) implements Command {}
}
