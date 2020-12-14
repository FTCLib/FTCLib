package com.arcrobotics.ftclib.hardware.motors;

import com.arcrobotics.ftclib.controller.PController;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * A continuous rotation servo that uses a motor object to
 * and a P controller to limit speed and acceleration.
 *
 * @author Jackson
 */
public class CRServo extends Motor {

    /**
     * The CR ServoEx motor object.
     */
    protected com.qualcomm.robotcore.hardware.CRServo crServo;

    /**
     * The P controller.
     */
    protected PController pController;

    /**
     * The constructor for the CR Servo.
     */
    public CRServo(HardwareMap hMap, String id) {
        this(hMap, id, 0.3);
    }

    /**
     * The constructor for the CR Servo that includes a custom
     * proportional error coefficient.
     *
     * @param kP    The desired coefficient for the P controller.
     */
    public CRServo(HardwareMap hMap, String id, double kP) {
        crServo = hMap.get(com.qualcomm.robotcore.hardware.CRServo.class, id);

        pController = new PController(kP);
    }

    @Override
    public void set(double speed) {
        crServo.setPower(get() + pController.calculate(get(), speed));
    }

    @Override
    public double get() {
        return crServo.getPower();
    }

    @Override
    public void setInverted(boolean isInverted) {
        crServo.setDirection(isInverted ? com.qualcomm.robotcore.hardware.CRServo.Direction.REVERSE
                : com.qualcomm.robotcore.hardware.CRServo.Direction.FORWARD);
    }

    @Override
    public boolean getInverted() {
        return crServo.getDirection() == com.qualcomm.robotcore.hardware.CRServo.Direction.REVERSE;
    }

    @Override
    public void disable() {
        crServo.close();
    }

    public void stop() {
        set(0);
    }

    @Override
    public void stopMotor() {
        stop();
    }

}
