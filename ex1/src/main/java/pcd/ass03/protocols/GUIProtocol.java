package pcd.ass03.protocols;

import pcd.ass03.model.BoidState;
import pcd.ass03.model.SimulationMetrics;

import java.util.List;

/**
 * Protocol for the GUI in the Boids simulation.
 * Defines commands for rendering frames and updating weights.
 */
public interface GUIProtocol {

    /**
     * Enum representing possible simulation states.
     */
    enum SimulationStatus {
        STARTING("Starting..."),
        RUNNING("Running"),
        PAUSED("Paused"),
        STOPPED("Stopped"),
        RESUMED("Running - Resumed");

        private final String displayText;

        SimulationStatus(String displayText) {
            this.displayText = displayText;
        }

        public String getDisplayText() {
            return displayText;
        }
    }

    interface Command {}

    /**
     * Command to render a frame in the GUI.
     * Contains the list of boids and the simulation metrics.
     *
     * @param boids the list of boid states to render
     * @param metrics the simulation metrics to display
     */
    record RenderFrame(List<BoidState> boids, SimulationMetrics metrics) implements Command {}

    /**
     * Command to update the weights for the boid behaviors.
     * This command is sent from the GUI to adjust the weights for separation, alignment, and cohesion.
     *
     * @param separationWeight the weight for separation behavior
     * @param alignmentWeight the weight for alignment behavior
     * @param cohesionWeight the weight for cohesion behavior
     */
    record UpdateWeights(double separationWeight,
                         double alignmentWeight,
                         double cohesionWeight) implements Command {}


    /**
     * Command sent when user click on the "Pause" button in the GUI.
     */
    record UserPause() implements Command {}

    /**
     * Command sent when user clicks resume button.
     */
    record UserResume() implements Command {}

    /**
     * Command sent when user clicks stop button.
     */
    record UserStop() implements Command {}

    /**
     * Command sent when user changes parameters via sliders.
     *
     * @param cohesion cohesion factor
     * @param alignment alignment factor
     * @param separation separation factor
     */
    record UserUpdateParams(double cohesion, double alignment, double separation) implements Command {}

    /**
     * Confirmation that simulation has been paused.
     */
    record ConfirmPause() implements Command {}

    /**
     * Confirmation that simulation has been resumed.
     */
    record ConfirmResume() implements Command {}

    /**
     * Confirmation that simulation has been stopped.
     */
    record ConfirmStop() implements Command {}

    /**
     * Confirmation that parameters have been updated.
     */
    record ConfirmParamsUpdate() implements Command {}

    /**
     * Update GUI with current simulation status.
     *
     * @param status the current status (RUNNING, PAUSED, STOPPED, etc.)
     */
    record UpdateStatus(SimulationStatus status) implements Command {}
}
