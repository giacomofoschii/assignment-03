package pcd.ass03.actors;

public class BoidsParams {
    private double separationWeight = 1.0;
    private double alignmentWeight = 1.0;
    private double cohesionWeight = 1.0;
    private final double maxSpeed = 4.0;
    private final double perceptionRadius = 50.0;
    private final double avoidRadius = 20.0;

    final static int N_BOIDS = 1500;

    final static double SEPARATION_WEIGHT = 1.0;
    final static double ALIGNMENT_WEIGHT = 1.0;
    final static double COHESION_WEIGHT = 1.0;

    final static int ENVIRONMENT_WIDTH = 1000;
    final static int ENVIRONMENT_HEIGHT = 1000;

    public double getAlignmentWeight() {
        return alignmentWeight;
    }

    public void setAlignmentWeight(double alignmentWeight) {
        this.alignmentWeight = alignmentWeight;
    }

    public double getSeparationWeight() {
        return separationWeight;
    }

    public void setSeparationWeight(double separationWeight) {
        this.separationWeight = separationWeight;
    }

    public double getCohesionWeight() {
        return cohesionWeight;
    }

    public void setCohesionWeight(double cohesionWeight) {
        this.cohesionWeight = cohesionWeight;
    }

    public double getAvoidRadius() {
        return avoidRadius;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public double getPerceptionRadius() {
        return perceptionRadius;
    }
}
