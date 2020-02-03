package com.arcrobotics.ftclib.hardware.motors;

/**
 * Allows multiple {@link Motor} objects to be linked together
 * as a single group. Multiple motors will act together.
 */
public class MotorGroup implements Motor {

    private Motor[] group;
    private boolean isInverted;

    /**
     * Create a new MotorGroup with the provided Motors.
     *
     * @param motors The motors to add.
     */
    public MotorGroup(Motor... motors) {
        group = motors;
    }

    @Override
    public void set(double speed) {
        for (Motor x : group) {
            x.set(isInverted ? -speed : speed);
        }
    }

    @Override
    public double get() {
        return group[0].get();
    }

    @Override
    public boolean getInverted() {
        return isInverted;
    }

    @Override
    public void setInverted(boolean isInverted) {
        this.isInverted = isInverted;
    }

    @Override
    public void disable() {
        for (Motor x : group) {
            x.disable();
        }
    }

    public String getDeviceType() {
        return "Motor Group";
    }

    @Override
    public void pidWrite(double output) {
        set(output);
    }

    @Override
    public void stopMotor() {
        for (Motor x : group) {
            x.stopMotor();
        }
    }

}
