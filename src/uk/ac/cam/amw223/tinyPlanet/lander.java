package uk.ac.cam.amw223.tinyPlanet;

import java.awt.*;

public class lander {

    vector2D position;
    vector2D velocity;
    vector2D acceleration;

    double pitch = 0.0;

    public lander(vector2D r) {
        position = r;
        velocity = new vector2D();
        acceleration = new vector2D();
    }

    public lander(vector2D r, vector2D v, double p) {
        position = r;
        velocity = v;
        acceleration = new vector2D();
        setPitch(p);
    }

    private void setPitch(double p) {
        // ensures pitch is always between 0 and 2 pi
        pitch = p % (Math.PI * 2);
    }

    public void draw(Graphics g, vector2D position) {

    }

}
