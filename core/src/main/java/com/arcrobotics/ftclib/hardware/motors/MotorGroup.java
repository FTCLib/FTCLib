package com.arcrobotics.ftclib.hardware.motors;

/**
 * Allows multiple {@link Motor} objects to be linked together
 * as a single group. Multiple motors will act together.
 *
 * @author Jackson
 */
public class MotorGroup extends Motor {

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

    /**
     * Set the speed for each motor in the group
     *
     * @param speed The speed to set. Value should be between -1.0 and 1.0.
     */
    @Override
    public void set(double speed) {
        for (Motor x : group) {
            x.set(isInverted ? -speed : speed);
        }
    }

    /**
     * @return The speed as a percentage of output
     */
    @Override
    public double get() {
        return group[0].get();
    }

    /**
     * @return true if the motor group is inverted
     */
    @Override
    public boolean getInverted() {
        return isInverted;
    }

    /**
     * Set the motor group to the inverted direction or forward direction.
     * This directly affects the speed rather than the direction.
     *
     * @param isInverted The state of inversion true is inverted.
     */
    @Override
    public void setInverted(boolean isInverted) {
        this.isInverted = isInverted;
    }

    /**
     * Disables all the motor devices.
     */
    @Override
    public void disable() {
        for (Motor x : group) {
            x.disable();
        }
    }

    /**
     * @return a string characterizing the device type
     */
    @Override
    public String getDeviceType() {
        return "Motor Group";
    }

    /**
     * Stops all motors in the group.
     */
    @Override
    public void stopMotor() {
        for (Motor x : group) {
            x.stopMotor();
        }
    }

}
