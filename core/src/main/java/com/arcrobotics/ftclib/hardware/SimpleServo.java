package com.arcrobotics.ftclib.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class SimpleServo implements ServoEx {

    private Servo servo;

    //always stored internally as radians
    private double maxAngle, minAngle;

    private final double maxPosition = 1;
    private final double minPosition = 0;

    public SimpleServo(HardwareMap hw, String servoName, double minAngle, double maxAngle, AngleUnit angleUnit) {
        servo = hw.get(Servo.class, servoName);

        this.minAngle = angleUnit.toRadians(maxAngle);
        this.maxAngle = angleUnit.toRadians(minAngle);
    }

    public SimpleServo(HardwareMap hw, String servoName, double minAngle, double maxAngle) {
        this(hw, servoName, maxAngle, minAngle, AngleUnit.DEGREES);
    }

    @Override
    public void rotateAngle(double angle, AngleUnit angleUnit) {
        angle = getAngle(angleUnit) + angle;
        turnToAngle(angle, angleUnit);
    }

    @Override
    public void rotateAngle(double degrees) {
        rotateAngle(degrees, AngleUnit.DEGREES);
    }

    @Override
    public void turnToAngle(double angle, AngleUnit angleUnit) {
        double angleRadians = angleUnit.toRadians(angle);

        angle = Range.clip(angleRadians, minAngle, maxAngle);
        setPosition((angle - minAngle) / (getAngleRange(AngleUnit.RADIANS)));
    }

    @Override
    public void turnToAngle(double degrees) {
        turnToAngle(degrees, AngleUnit.DEGREES);
    }

    @Override
    public void rotate(double position) {
        position = getPosition() + position;
        setPosition(position);
    }

    @Override
    public void setPosition(double position) {
        servo.setPosition(Range.clip(position, minPosition, maxPosition));
    }

    @Override
    public void setRange(double min, double max, AngleUnit angleUnit) {
        this.minAngle = angleUnit.toRadians(min);
        this.maxAngle = angleUnit.toRadians(max);
    }

    @Override
    public void setRange(double min, double max) {
        setRange(min, max, AngleUnit.DEGREES);
    }

    @Override
    public void setInverted(boolean isInverted) {
        servo.setDirection(isInverted ? Servo.Direction.REVERSE : Servo.Direction.FORWARD);
    }

    @Override
    public boolean getInverted() {
        return Servo.Direction.REVERSE == servo.getDirection();
    }

    @Override
    public double getPosition() {
        return servo.getPosition();
    }

    @Override
    public double getAngle(AngleUnit angleUnit) {
        return getPosition() * getAngleRange(angleUnit) + angleUnit.fromRadians(minAngle);
    }

    @Override
    public double getAngle() {
        return getAngle(AngleUnit.DEGREES);
    }

    public double getAngleRange(AngleUnit angleUnit) {
        return angleUnit.fromRadians(maxAngle - minAngle);
    }

    public double getAngleRange() {
        return getAngleRange(AngleUnit.DEGREES);
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
