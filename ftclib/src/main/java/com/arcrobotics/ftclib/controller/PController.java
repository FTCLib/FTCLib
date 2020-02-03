package com.arcrobotics.ftclib.controller;

import com.arcrobotics.ftclib.hardware.motors.Motor;

public class PController extends PIDFController {

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

    /**
     * Implements a p control calculation onto the affected motor.
     *
     * @param affected  The affected motor of the mechanism.
     * @param sp        The setpoint of the calculation.
     * @param pv        The previous value of the calculation.
     * @param speed     The maximum speed the motor should rotate.
     */
    public void pControl(Motor affected, double sp, double pv, double speed) {
        if (Math.abs(sp) > Math.abs(pv)) affected.set(speed * super.calculate(pv, sp));
        else affected.set(0);
    }

    /**
     * Implements a p control calculation onto the affected motor.
     *
     * @param affected  The affected motor of the mechanism.
     * @param sp        The setpoint of the calculation.
     * @param pv        The previous value of the calculation.
     */
    public void pControl(Motor affected, double sp, double pv) {
        pControl(affected, sp, pv, 1);
    }

}
