package com.arcrobotics.ftclib.hardware.motors;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class GenericMotorEx implements Motor {
    public DcMotorEx dcMotor;
    double distancePerPulse;

    public GenericMotorEx(HardwareMap hw, String motorName) {
        dcMotor = (DcMotorEx) hw.dcMotor.get(motorName);
        dcMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // Not set yet
        distancePerPulse = -1;
    }


    @Override
    public void set(double speed) {
        dcMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        dcMotor.setPower(speed);
    }

    @Override
    public double get() {
        return dcMotor.getPower();
    }

    @Override
    public void setInverted(boolean isInverted) {
        if(isInverted)
            dcMotor.setDirection(DcMotorSimple.Direction.REVERSE);
        else
            dcMotor.setDirection(DcMotorSimple.Direction.FORWARD);

    }

    @Override
    public boolean getInverted() {
        if(dcMotor.getDirection() == DcMotorSimple.Direction.REVERSE)
            return true;
        else
            return false;
    }

    @Override
    public void disable() {
        stopMotor();
    }

    @Override
    public String getDeviceType() {
        return dcMotor.getDeviceName();
    }

    @Override
    public void pidWrite(double output) {
        dcMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        dcMotor.setPower(output);
    }

    @Override
    public void stopMotor() {
        dcMotor.setPower(0);
    }

    /**
     * Sets how far a motor will travel per encoder pulse
     *
     * @param distancePerPulse
     */
    public void setDistancePerPulse(double distancePerPulse) {
        this.distancePerPulse = Math.abs(distancePerPulse);
    }


    public int getEncoderPulses(double distancePerPulse) {
        return dcMotor.getCurrentPosition();
    }

    public double getDistance() throws Exception {
        // User has not set distance per pulse
        if(distancePerPulse == -1) {
            throw new Exception("Must set distance per pulse or call" +
                    " methods that set distance per pulse before getting distance!");
        } else {
            return distancePerPulse * dcMotor.getCurrentPosition();
        }
    }

    public void setTargetPosition(int pulses) {
        dcMotor.setTargetPosition(pulses);
        dcMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
    }

    public void setTargetDistance(double distance) throws Exception {
        // User has not set distance per pulse
        if(distancePerPulse == -1) {
            throw new Exception("Must set distance per pulse or call" +
                    " methods that set distance per pulse before setting distance!");
        } else {
            setTargetPosition((int) ((1 / distancePerPulse) * distance));
        }
    }

    public void resetEncoder() {
        DcMotor.RunMode currentRunMode = dcMotor.getMode();
        dcMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        dcMotor.setMode(currentRunMode);
    }

    public boolean isBusy() {
        return dcMotor.isBusy();
    }

}
