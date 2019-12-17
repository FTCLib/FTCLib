package com.arcrobotics.ftclib.hardware.motors;

import com.arcrobotics.ftclib.controller.PController;

/**
 * A continuous rotation servo that uses a motor object to
 * and a P controller to limit speed and acceleration.
 */
public class CRServo implements Motor {

    /**
     * The CR ServoEx motor object.
     */
    private Motor crServo;

    /**
     * The P controller.
     */
    private PController pController;

    /**
     * The constructor for the CR ServoEx.
     *
     * @param servo The servo in question.
     */
    public CRServo(Motor servo) {
        crServo = servo;

        pController = new PController(0.3);
    }

    @Override
    public void set(double speed) {
        pController.pControl(crServo, speed, get(), 0.5);
    }

    @Override
    public double get() {
        return crServo.get();
    }

    @Override
    public void setInverted(boolean isInverted) {
        crServo.setInverted(isInverted);
    }

    @Override
    public boolean getInverted() {
        return crServo.getInverted();
    }

    @Override
    public void disable() {
        crServo.disable();
    }

    @Override
    public String getDeviceType() {
        return "CR ServoEx";
    }

    @Override
    public void pidWrite(double output) {
        set(output);
    }

    @Override
    public void stopMotor() {
        set(0);
    }

}
