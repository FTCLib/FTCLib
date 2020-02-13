package com.arcrobotics.ftclib.hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class JSTEncoder extends ExternalEncoder {

    private DcMotorEx encoder;
    /**
     * counts: Current encoder counts
     * offset: Offset encoder to "sync"
     * dpp: Distance per pulse
     */
    int counts, offset, multiplier;
    double dpp;

    public JSTEncoder(HardwareMap hw, String encoderName) {
        encoder = (DcMotorEx) hw.get(DcMotor.class, encoderName);
        encoder.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        encoder.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        counts = 0;
        offset = 0;
        multiplier = 1;
        dpp = 0;
    }

    /**
     * How much distance is covered in one tick of an encoder.
     * For Drive: dpp = (WHEEL_DIAMETER*Math.PI) / cpr
     * @param dpp
     */
    public void setDistancePerPulse(double dpp) {
        this.dpp = dpp;
    }

    public double getDistance() {
        return dpp * getCounts();
    }

    public void setInverted(boolean isInverted) {
        if(isInverted)
            multiplier = -1;
        else
            multiplier = 1;
    }

    public boolean getInverted() {
        if(multiplier == -1)
            return true;
        else
            return false;
    }

    @Override
    public long getCounts() {
        counts = (encoder.getCurrentPosition() + offset) * multiplier;
        return counts;
    }

    @Override
    public void syncEncoder() {
        offset = (int) -getCounts();
    }

    @Override
    public void resetEncoder() {
        encoder.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        offset = 0;
        encoder.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    public double getRate() {
        return encoder.getVelocity() * dpp * multiplier;
    }
}
