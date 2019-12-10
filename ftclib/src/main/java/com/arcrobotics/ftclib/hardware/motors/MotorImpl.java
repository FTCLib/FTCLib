package com.arcrobotics.ftclib.hardware.motors;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class MotorImpl {

    Motor mot;

    public class CustomMotor implements Motor {

        private DcMotorEx motor;
        private double counts_per_rev;

        public CustomMotor(HardwareMap hMap, String motorName, double cpr) {
            motor = (DcMotorEx)hMap.get(DcMotor.class, motorName);
            counts_per_rev = cpr;
        }

        @Override
        public void set(double speed) {
            motor.setVelocity(counts_per_rev * speed);
        }

        @Override
        public double get() {
            return motor.getVelocity();
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
            motor.setMotorDisable();
        }

        @Override
        public String getDeviceType() {
            return motor.getDeviceName();
        }

        @Override
        public void pidWrite(double output) {
            motor.setVelocity(output);
        }

        @Override
        public void stopMotor() {
            set(0);
        }

        public double getCurrentPosition() {
            return motor.getCurrentPosition();
        }

        public double getTargetPosition() {
            return motor.getTargetPosition();
        }

    }

    public MotorImpl(HardwareMap hMap, String motorName, double cpr) {
        mot = new CustomMotor(hMap, motorName, cpr);
    }

    void set(double speed) {
        mot.set(speed);
    }

    double getCurrentPosition() {
        return ((CustomMotor)mot).getCurrentPosition();
    }

    public double get() {
        return mot.get();
    }

    public void setInverted(boolean isInverted) {
        mot.setInverted(isInverted);
    }

    public boolean getInverted() {
        return mot.getInverted();
    }

    public void disable() {
        mot.disable();
    }

    public void pidWrite(double output) {
        mot.pidWrite(output);
    }

    public String getDeviceType() {
        return mot.getDeviceType();
    }

}
