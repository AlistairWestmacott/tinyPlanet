package uk.ac.cam.amw223.sjp2d;

public class sjpBody {

    private sjpVector2D position = null;
    private sjpVector2D velocity = null;
    private sjpVector2D acceleration = null;

    double mass = 0.0;

    // reference to the universe for things like constants
    sjpUniverse universe;

    public sjpBody(sjpUniverse universe) {
        position = new sjpVector2D();
        velocity = new sjpVector2D();
        acceleration = new sjpVector2D();
        this.universe = universe;
    }

    public void update(double dt) {
        acceleration = universe.gravitationalPotential(position);
        velocity = velocity.add(acceleration.scale(dt));
        position = position.add(velocity.scale(dt)).add(acceleration.scale(dt * dt / 2));
    }

    public double m() {
        return mass;
    }

    public sjpVector2D r() {
        return position;
    }

    public sjpVector2D v() {
        return velocity;
    }

    public sjpVector2D a() {
        return acceleration;
    }

    // use the reference to universe to generate gravitational potential at position
    // then use this to calculate direction of acceleration.

}
