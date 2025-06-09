package pcd.ass03.actors;

public class BoidsParams {
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
        return width;
    }

    public double getHeight() {
        return height;
    }

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
        return 20.0;
    }

    public double getMaxSpeed() {
        return 4.0;
    }

    public double getPerceptionRadius() {
        return 50.0;
    }

    public double getMinX() {
        return -width/2;
    }

    public double getMaxX() {
        return width/2;
    }

    public double getMinY() {
        return -height/2;
    }

    public double getMaxY() {
        return height/2;
    }
}
