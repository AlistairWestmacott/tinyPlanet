package uk.ac.cam.amw223.tinyPlanet;

public class vector2D {
    private double components[];

    public vector2D(double x, double y) {
        components = new double[]{x, y};
    }

    public vector2D() {
        components = new double[]{0, 0};
    }

    public vector2D(vector2D v) {
        // make a copy of v
        components = new double[]{v.x(), v.y()};
    }

    public void add(vector2D v1) {
        components[0] += v1.x();
        components[1] += + v1.y();
    }

    public void scale(double s) {
        components[0] *= s;
        components[1] *= s;
    }

    public double mag() {
        // magnitude of this
        return Math.sqrt(components[0] * components[0] + components[1] * components[1]);
    }

    public double dot(vector2D v) {
        return v.x() * this.x() + v.y() * this.y();
    }

    public void normalise() {
        this.scale(1/this.mag());
    }

    public double x() {
        return components[0];
    }

    public double y() {
        return components[1];
    }

    public void setX(double x) {
        components[0] = x;
    }

    public void setY(double y) {
        components[1] = y;
    }

}
