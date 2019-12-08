package com.arcrobotics.ftclib.controller;

public class PController extends PIDFController {

    public PController(double p, double sp, double pv, double period) {
        super(new double[]{p, 0, 0, 0}, sp, pv, period);
    }

}
