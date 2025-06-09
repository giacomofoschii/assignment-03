package pcd.ass03.protocols;

import pcd.ass03.model.BoidState;
import pcd.ass03.model.SimulationMetrics;

import java.util.List;

public interface GUIProtocol {

    interface Command {}

    record RenderFrame(List<BoidState> boids, SimulationMetrics metrics) implements Command {}
}
