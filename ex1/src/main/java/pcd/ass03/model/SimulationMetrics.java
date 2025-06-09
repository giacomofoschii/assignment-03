package pcd.ass03.model;

public record SimulationMetrics(int totalBoids, double fps,
                                long tickNumber, double avgNeighbors) {}