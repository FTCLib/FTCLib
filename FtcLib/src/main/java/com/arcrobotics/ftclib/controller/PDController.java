package com.arcrobotics.ftclib.controller;

import com.arcrobotics.ftclib.controller.PIDController;

public class PDController extends PIDController {



    /**
     * Default constructor, just takes a 2 element array containing the {P, D} values
     *
     * @param coeffs 2 element array with the PID coefficients
     */
    public PDController(double[] coeffs){
        super(new double[] {coeffs[0], coeffs[1], 0, 0});
    }

    /**
     * The extended constructor.
     */
    public PDController(double[] coeffs, double sp, double pv, double period) {
        super(new double[]{coeffs[0], coeffs[1], 0, 0}, sp, pv, period);
    }
}
