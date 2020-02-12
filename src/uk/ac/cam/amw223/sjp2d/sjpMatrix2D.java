package uk.ac.cam.amw223.sjp2d;

import java.util.ArrayList;
import java.util.List;

public class sjpMatrix2D {

    double[][] components = new double[2][2];


    static class determinantCalculator extends Thread {

        List<sjpMatrix2D> jobQueue = new ArrayList<sjpMatrix2D>();

        @Override
        public void run() {

        }
    }

    private boolean determinantCalc= false;


}
