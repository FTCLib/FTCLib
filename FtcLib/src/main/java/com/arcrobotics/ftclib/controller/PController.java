package com.arcrobotics.ftclib.controller;

import com.arcrobotics.ftclib.hardware.motors.Motor;

public class PController extends PDController {

    /**
     * Default constructor, only takes a p-value.
     *
     * @param p The value of kP for the coefficients.
     */
    public PController(double p) {
        super(new double[]{p, 0, 0, 0});
    }

    /**
     * The extended constructor.
     */
    public PController(double p, double sp, double pv, double period) {
        super(new double[]{p, 0, 0, 0}, sp, pv, period);
    }

}
