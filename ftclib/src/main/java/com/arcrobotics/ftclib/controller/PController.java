package com.arcrobotics.ftclib.controller;

import com.arcrobotics.ftclib.hardware.Motor;

public class PController extends PIDFController {

    public PController(double p) {
        super(new double[]{p, 0, 0, 0});
    }

    public PController(double p, double sp, double pv, double period) {
        super(new double[]{p, 0, 0, 0}, sp, pv, period);
    }

    public void pControl(Motor affected, double sp, double pv) {
        affected.set(super.calculate(pv, sp));
    }

}
