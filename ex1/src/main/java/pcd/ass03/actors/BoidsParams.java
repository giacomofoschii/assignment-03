package pcd.ass03.actors;

public class BoidsParams {
    private static final double AVOID_RADIUS = 20.0;
    private static final double MAX_SPEED = 4.0;
    private static final double PERCEPTION_RADIUS = 50.0;

    private double separationWeight = 1.0;
    private double alignmentWeight = 1.0;
    private double cohesionWeight = 1.0;

    private final double width;
    private final double height;

    public BoidsParams(final double width, final double height) {
        this.height = height;
        this.width = width;
    }

    public double getWidth() {
        return this.width;
    }

    public double getHeight() {
        return this.height;
    }

    public double getAlignmentWeight() {
        return this.alignmentWeight;
    }

    public void setAlignmentWeight(double alignmentWeight) {
        this.alignmentWeight = alignmentWeight;
    }

    public double getSeparationWeight() {
        return this.separationWeight;
    }

    public void setSeparationWeight(double separationWeight) {
        this.separationWeight = separationWeight;
    }

    public double getCohesionWeight() {
        return this.cohesionWeight;
    }

    public void setCohesionWeight(double cohesionWeight) {
        this.cohesionWeight = cohesionWeight;
    }

    public double getAvoidRadius() {
        return AVOID_RADIUS;
    }

    public double getMaxSpeed() {
        return MAX_SPEED;
    }

    public double getPerceptionRadius() {
        return PERCEPTION_RADIUS;
    }

    public double getMinX() {
        return -this.width/2;
    }

    public double getMaxX() {
        return this.width/2;
    }

    public double getMinY() {
        return -this.height/2;
    }

    public double getMaxY() {
        return this.height/2;
    }
}
