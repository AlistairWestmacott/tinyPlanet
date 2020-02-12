package uk.ac.cam.amw223.sjp2d;

public class sjpVector2D {

    private double components[];

    boolean magnitudeCalculated = false;
    double magnitude = 0.0;// magnitude is cached to  prevent repeated calculation

    public sjpVector2D(double x, double y) {
        components = new double[]{x, y};
    }

    public sjpVector2D() {
        components = new double[]{0, 0};
    }

    public sjpVector2D(sjpVector2D v) {
        // make a copy of v
        components = new double[]{v.x(), v.y()};
    }

    public sjpVector2D scale(double s) {
        return new sjpVector2D(components[0] *= s,
                components[1] *= s);
    }

    public double mag() {
        // magnitude of this
        if (!magnitudeCalculated) {
            magnitude = Math.sqrt(components[0] * components[0] + components[1] * components[1]);
            magnitudeCalculated = true;
        }
        return magnitude;
    }

    public double dot(sjpVector2D v) {
        return v.x() * this.x() + v.y() * this.y();
    }

    public sjpVector2D add(sjpVector2D v) {
        return new sjpVector2D(components[0] + v.x(), components[1] + v.y());
    }

    public sjpVector2D sub(sjpVector2D v) {
        return new sjpVector2D(components[0] - v.x(), components[1] - v.y());
    }

    public sjpVector2D normalise() {
        return this.scale(1 / this.mag());
    }

    public double x() {
        return components[0];
    }

    public double y() {
        return components[1];
    }

    public void setX(double x) {
        components[0] = x;
        magnitudeCalculated = false;
    }

    public void setY(double y) {
        components[1] = y;
        magnitudeCalculated = false;
    }
}