package com.arcrobotics.ftclib.hardware.motors;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * A simple example of a {@link Motor} object.
 *
 * @author Jackson
 */
public class SimpleMotor implements Motor {

    private DcMotor motor;

    /**
     * Creates an {@link DcMotor} object using the given name
     * in the configuration file.
     *
     * @param name the name of the motor in the RC config
     * @param hMap the hardware map that locates the designated device
     */
    public SimpleMotor(String name, HardwareMap hMap) {
        this(hMap.get(DcMotor.class, name));
    }

    /**
     * Points the motor to a specific {@link DcMotor}.
     *
     * @param motor the motor being used for the Motor object
     */
    public SimpleMotor(DcMotor motor) {
        this.motor = motor;
    }

    @Override
    public void set(double speed) {
        motor.setPower(speed);
    }

    @Override
    public double get() {
        return motor.getPower();
    }

    @Override
    public void setInverted(boolean isInverted) {
        motor.setDirection(isInverted ? DcMotor.Direction.REVERSE : DcMotor.Direction.FORWARD);
    }

    @Override
    public boolean getInverted() {
        return motor.getDirection() == DcMotor.Direction.REVERSE;
    }

    @Override
    public void disable() {
        motor.close();
    }

    /**
     * @return the name of the device and its port number as a {@link String}.
     */
    @Override
    public String getDeviceType() {
        return motor.getDeviceName() + " at port " + motor.getPortNumber();
    }

    /**
     * Adds a layer of control to the motor speed by introducing
     * P control. See {@link com.arcrobotics.ftclib.controller.PController} for more.
     *
     * @param output the desired output speed
     */
    @Override
    public void pidWrite(double output) {
        set( (output - get()) / output );
    }

    @Override
    public void stopMotor() {
        set(0);
    }

    /**
     * @return The current position of the output shaft in ticks
     */
    public int getCurrentPosition() {
        return motor.getCurrentPosition();
    }

}
