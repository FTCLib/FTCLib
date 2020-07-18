package com.arcrobotics.ftclib.controller;


public class PIDController extends PIDFController {


    /**
     * Default constructor, just takes a 3 element array containing the {P, I, D} values
     *
     * @param coeffs 3 element array with the PID coefficients
     */
    public PIDController(double[] coeffs){
        super(new double[] {coeffs[0], coeffs[1], coeffs[2], 0});
    }

    /**
     * The extended constructor.
     */
    public PIDController(double[] coeffs, double sp, double pv, double period) {
        super(new double[]{coeffs[0], coeffs[1], coeffs[2], 0}, sp, pv, period);
    }

    public void setPID(double kp, double ki, double kd) {
        setPIDF(kp,ki,kd,0);
    }

}
