package com.arcrobotics.ftclib.hardware;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

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

        if(angleUnit == AngleUnit.DEGREES) {
            this.maxAngle = Math.toRadians(maxAngle);
            this.minAngle = Math.toRadians(minAngle);
        } else {
            this.maxAngle = maxAngle;
            this.minAngle = minAngle;
        }

        this.angleUnit = angleUnit;

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
        double iMaxAngle = maxAngle;
        double iMinAngle = minAngle;

        if(angleUnit == AngleUnit.DEGREES) {
            iMaxAngle = Math.toDegrees(iMaxAngle);
            iMinAngle = Math.toDegrees(iMinAngle);
        }

        if(angle > iMaxAngle)
            angle = iMaxAngle;
        else if(angle < iMinAngle)
            angle = iMinAngle;

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
        if(angleUnit == AngleUnit.DEGREES) {
            min = Math.toRadians(min);
            max = Math.toRadians(max);
        }
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
        return Servo.Direction.REVERSE == servo.getDirection();
    }

    @Override
    public double getPosition() {
        return servo.getPosition();
    }

    @Override
    public double getAngle() {
        double angleRadians = getPosition() * getAngleRange() + minAngle;
        if(angleUnit == AngleUnit.DEGREES) {
            return Math.toDegrees(angleRadians);
        } else {
            return angleRadians;
        }
    }

    public double getAngleRange() {
        double angleRangeRadians = maxAngle - minAngle;
        if(angleUnit == AngleUnit.DEGREES) {
            return Math.toDegrees(angleRangeRadians);
        } else {
            return angleRangeRadians;
        }
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
