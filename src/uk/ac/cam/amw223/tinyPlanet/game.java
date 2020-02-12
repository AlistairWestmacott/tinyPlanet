package uk.ac.cam.amw223.tinyPlanet;

import javax.swing.*;
import java.awt.*;

public class game extends Canvas {

    level mainLevel = new level();

    public game() {

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("My Drawing");
        Canvas canvas = new Drawing();

        game mainGame = new game();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.setSize(400, 400);
        canvas.setBackground(Color.black);
        frame.add(canvas);
        frame.pack();
        frame.setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        mainLevel.draw(g, new vector2D());
    }

}
