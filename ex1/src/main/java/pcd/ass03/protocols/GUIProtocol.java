package pcd.ass03.protocols;

import pcd.ass03.model.BoidState;
import pcd.ass03.model.SimulationMetrics;

import java.util.List;

/**
 * Protocol for the GUI in the Boids simulation.
 * Defines commands for rendering frames and updating weights.
 */
public interface GUIProtocol {

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
}
