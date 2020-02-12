package uk.ac.cam.amw223.tinyPlanet;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class level {

    List<Object> scene = new ArrayList<>();

    // universal gravitational constant
    static final double G = 6.67E-11;

    int windowWidth = 400;
    int windowHeight = 400;

    lander LM = new lander(new vector2D());

    double constants[];

    public level() {
        // need a good way of generating constants
        // modular arithmetic might be a good way of
        // generating good, random-looking landscapes
        // consistently

        constants = new double[]{1.1, 1.2, 1.3, 1.4};
    }

    public double f(double x) {
        return constants[0] * Math.sin(constants[1] * x) + constants[2] * Math.cos(constants[3] * x);
    }

    public vector2D normalAt(double x) {
        // this uses the derivative of the function f(x)
        return new vector2D(1, constants[0] * constants[1] * Math.cos(x * constants[1]) -
                constants[2] * constants[3] * Math.sin(x * constants[3]));
    }

    public void draw(Graphics g, vector2D position) {
        double x = position.x();
        double y = 0;
        int yCoord = 0;

        g.setColor(Color.white);

        for (int i = 0; i < windowWidth; i++) {
            y = f(x);
            yCoord = (int)Math.floor(y - position.y());
            if (yCoord < windowHeight/2) {
                g.drawLine(i, yCoord, i, yCoord);
            }
        }

        LM.draw(g, new vector2D(windowWidth/2, windowHeight/2));

    }

}
