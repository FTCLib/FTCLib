package com.arcrobotics.ftclib.hardware.motors;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;


public class SimpleMotorImpl implements Motor {

    private DcMotorEx motor;
    double cpr;

    Telemetry telemetry;

    public SimpleMotorImpl(HardwareMap hMap, Telemetry telemetry, String motorName) {
        motor = (DcMotorEx)hMap.get(DcMotor.class, motorName);
        cpr = 0;
        this.telemetry = telemetry;
    }

    public SimpleMotorImpl(HardwareMap hMap, Telemetry telemetry, String motorName, double cpr) {
        motor = (DcMotorEx)hMap.get(DcMotor.class, motorName);
        this.cpr = cpr;
        this.telemetry = telemetry;
    }


    public void setCpr(int cpr) {
        this.cpr = cpr;
    }

    public double getCpr() {
        return cpr;
    }

    public void setExternalGearReduction(double gearReduction) {
        cpr = (int) (cpr * gearReduction);
    }

    public void reset() {
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    }

    public int getRaw() {
        return motor.getCurrentPosition();
    }

    public void setRPM(double rpm) throws Exception {
        if(cpr == 0) {
            throw new Exception("Must set counts per rotation");
        }

        if(motor.getMode() != DcMotor.RunMode.RUN_TO_POSITION) {
            motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }

        // Convert it to ticks per second
        motor.setVelocity((rpm*cpr)/60);
    }

    public void setDistance(double target, double wheelDiameter) {
        try {
            motor.setTargetPosition((int) (getRotations()*wheelDiameter*Math.PI));
            motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        } catch(Exception e) {
            telemetry.addData("Error: ", e.getMessage());
            telemetry.update();
            e.printStackTrace();
        }

    }

    public double getRotations() throws Exception {
        if(cpr == 0) {
            throw new Exception("Must set counts per rotation");
        }

        return motor.getCurrentPosition() / cpr;

    }

    public boolean isBusy() {
        return motor.isBusy();
    }

    @Override
    public void set(double speed) {
        // Might need to change this to RUN_WITHOUT ENCODER
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motor.setPower(speed);
    }

    @Override
    public double get() {
        return motor.getPower();
    }

    @Override
    public void setInverted(boolean isInverted) {
        if(isInverted)
            motor.setDirection(DcMotorSimple.Direction.REVERSE);
        else
            motor.setDirection(DcMotorSimple.Direction.FORWARD);
    }

    @Override
    public boolean getInverted() {
        if(motor.getDirection() == DcMotorSimple.Direction.REVERSE)
            return true;
        else
            return false;
    }

    @Override
    public void disable() {
        motor.setMotorDisable();
    }

    @Override
    public String getDeviceType() {
        String port = Integer.toString(motor.getPortNumber());
        String controller = motor.getController().toString();
        return "SimpleMotorImpl: " + port + "; " + controller;
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
