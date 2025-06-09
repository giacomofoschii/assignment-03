package pcd.ass03.protocols;

import pcd.ass03.model.SimulationMetrics;

public interface GUIProtocol {

    interface Command {}

    record RenderFrame(List<BoidState> boids,
                       SimulationMetrics metrics) implements Command {}
}
