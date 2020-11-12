package com.arcrobotics.ftclib.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

public class SimpleServo implements ServoEx {

    Servo servo;
    double maxAngle, minAngle;
    final double maxPosition = 1;
    final double minPosition = 0;

    public SimpleServo(HardwareMap hw, String servoName) {
        servo = hw.get(Servo.class, servoName);
        maxAngle = 180;
        minAngle = 0;
    }

    public SimpleServo(HardwareMap hw, String servoName, double maxAngle, double minAngle) {
        servo = hw.get(Servo.class, servoName);
        this.maxAngle = maxAngle;
        this.minAngle = minAngle;
    }


    @Override
    public void rotateDegrees(double angle) {
        angle = getAngle() + angle;
        turnToAngle(angle);
    }

    @Override
    public void turnToAngle(double angle) {
        if(angle > maxAngle)
            angle = maxAngle;
        else if(angle < minAngle)
            angle = minAngle;

        setPosition((angle - minAngle) / (getAngleRange()));
    }

    @Override
    public void rotate(double position) {
        position = getPosition() + position;
        setPosition(position);
    }

    @Override
    public void setPosition(double position) {
        if(position > maxPosition)
            servo.setPosition(maxPosition);
        else if(position < minAngle)
            servo.setPosition(minPosition);
        else
            servo.setPosition(position);
    }

    @Override
    public void setRange(double min, double max) {
        this.minAngle = min;
        this.maxAngle = max;
    }

    @Override
    public void setInverted(boolean isInverted) {
        if(isInverted)
            servo.setDirection(Servo.Direction.REVERSE);
        else
            servo.setDirection(Servo.Direction.FORWARD);

    }

    @Override
    public boolean getInverted() {
        if(Servo.Direction.REVERSE == servo.getDirection())
            return true;
        else
            return false;
    }

    @Override
    public double getPosition() {
        return servo.getPosition();
    }

    @Override
    public double getAngle() {
        return getPosition() * getAngleRange() + minAngle;
    }

    public double getAngleRange() {
        return maxAngle - minAngle;
    }

    @Override
    public void disable() {
        servo.close();
    }

    @Override
    public String getDeviceType() {
        String port = Integer.toString(servo.getPortNumber());
        String controller = servo.getController().toString();
        return "SimpleServo: " + port + "; " + controller;
    }

}
