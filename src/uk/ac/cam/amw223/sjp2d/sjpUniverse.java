package uk.ac.cam.amw223.sjp2d;

import java.util.ArrayList;
import java.util.List;

public class sjpUniverse {

    List<sjpBody> bodies = new ArrayList<sjpBody>();

    private static final double G = 6.67E-11;

    private double distanceBetween(sjpVector2D v1, sjpVector2D v2) {
        return Math.hypot(Math.abs(v1.x() - v2.x()), Math.abs(v1.y() - v2.y()));
    }

    public sjpVector2D gravitationalPotential(sjpVector2D r) {
        sjpVector2D result = new sjpVector2D();
        for (sjpBody body : bodies) {
            // this is completely wrong
            result = result.add((r.sub(body.r())).scale(body.m() * G / distanceBetween(r, body.r())));
        }
        return result;
    }
}
