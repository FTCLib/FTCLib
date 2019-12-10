package com.arcrobotics.ftclib.hardware.motors;

import com.arcrobotics.ftclib.controller.PIDFController;

public class MotorImplEx extends MotorEx {

    private MotorImpl motor;
    private double distancePerPulse;

    public MotorImplEx(MotorImpl ex, double cpr) {
        this(ex, cpr, new PIDFController(new double[]{0,0,0,0}));
    }

    public MotorImplEx(MotorImpl ex, double cpr, PIDFController pidfController) {
        super(ex.mot, cpr, pidfController);

        motor = ex;
        distancePerPulse = -1; // not set yet
    }

    @Override
    double getCurrentPosition() {
        return motor.getCurrentPosition();
    }

    public void setDistancePerPulse(double distancePerPulse) {
        this.distancePerPulse = Math.abs(distancePerPulse);
    }

    public double getEncoderPulses() {
        return getCurrentPosition();
    }

    public double getDistance() throws Exception {
        if(distancePerPulse == -1) {
            throw new Exception("Must set distance per pulse or call" +
                    " methods that set distance per pulse before getting distance!");
        }

        return distancePerPulse * getCurrentPosition();
    }

    public void setTargetDistance(double distance) throws Exception {
        if (distancePerPulse == -1) {
            throw new Exception("Must set distance per pulse or call" +
                    " methods that set distance per pulse before setting distance!");
        }

        setTargetPosition(distance / distancePerPulse);
    }

    @Override
    public double get() {
        return motor.get();
    }

    @Override
    public void setInverted(boolean isInverted) {
        motor.setInverted(isInverted);
    }

    @Override
    public boolean getInverted() {
        return motor.getInverted();
    }

    @Override
    public void disable() {
        motor.disable();
    }

    @Override
    public String getDeviceType() {
        return motor.getDeviceType();
    }

    @Override
    public void pidWrite(double output) {
        motor.pidWrite(output);
    }


}
