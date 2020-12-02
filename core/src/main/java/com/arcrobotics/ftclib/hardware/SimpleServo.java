package com.arcrobotics.ftclib.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class SimpleServo implements ServoEx {

    Servo servo;
    AngleUnit angleUnit;

    //always stored internally as radians
    double maxAngle, minAngle;

    final double maxPosition = 1;
    final double minPosition = 0;

    public SimpleServo(HardwareMap hw, String servoName, AngleUnit angleUnit) {
        servo = hw.get(Servo.class, servoName);
        this.angleUnit = angleUnit;

        maxAngle = Math.toRadians(180);
        minAngle = 0;
    }

    public SimpleServo(HardwareMap hw, String servoName){
        this(hw, servoName, AngleUnit.DEGREES);
    }

    public SimpleServo(HardwareMap hw, String servoName, double maxAngle, double minAngle, AngleUnit angleUnit) {
        servo = hw.get(Servo.class, servoName);
        this.angleUnit = angleUnit;

        this.maxAngle = angleUnit == AngleUnit.DEGREES ? Math.toRadians(maxAngle) : maxAngle;
        this.minAngle = angleUnit == AngleUnit.DEGREES ? Math.toRadians(minAngle) : minAngle;
    }

    public SimpleServo(HardwareMap hw, String servoName, double maxAngle, double minAngle) {
        this(hw, servoName, maxAngle, minAngle, AngleUnit.DEGREES);
    }

    @Override
    public void rotateAngle(double angle) {
        angle = getAngle() + angle;
        turnToAngle(angle);
    }

    @Override
    public void turnToAngle(double angle) {

        //use local variable in case we need to convert units to degrees
        //(remember these values are always stored as radians internally)
        double iMaxAngle = angleUnit == AngleUnit.DEGREES ? AngleUnit.normalizeDegrees(angleUnit.toDegrees(maxAngle)) : maxAngle;
        double iMinAngle = angleUnit == AngleUnit.DEGREES ? AngleUnit.normalizeDegrees(angleUnit.toDegrees(minAngle)) : minAngle;

        angle = Range.clip(angle, iMinAngle, iMaxAngle);

        setPosition((angle - iMinAngle) / (getAngleRange()));

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
    public void setAngleUnit(AngleUnit angleUnit) {
        this.angleUnit = angleUnit;
    }

    @Override
    public void setRange(double min, double max) {
        this.minAngle = angleUnit == AngleUnit.DEGREES ? Math.toRadians(min) : min;
        this.maxAngle = angleUnit == AngleUnit.DEGREES ? Math.toRadians(max) : max;
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
    public double getAngle() {
        //use local variable in case we need to convert units to degrees
        //(remember this value is always stored as radians internally)
        double iMinAngle = angleUnit == AngleUnit.DEGREES ? Math.toDegrees(minAngle) : minAngle;
        return getPosition() * getAngleRange() + iMinAngle;
    }

    public double getAngleRange() {
        double angleRangeRadians = maxAngle - minAngle;
        return angleUnit == AngleUnit.DEGREES ? Math.toDegrees(angleRangeRadians) : angleRangeRadians;
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
