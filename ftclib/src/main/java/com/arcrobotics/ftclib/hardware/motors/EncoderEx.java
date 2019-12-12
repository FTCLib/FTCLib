package com.arcrobotics.ftclib.hardware.motors;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.arcrobotics.ftclib.hardware.HardwareDevice;

/**
 * An extended encoder object. EncoderEx functions like a normal internal encoder
 * but has more methods and customization than a regular encoder. The primary purpose of this
 * classfile is for PIDF control {@link PIDFController}.
 *
 * <p>Uses a {@link MotorEx} object for encoder values.</p>
 */
public class EncoderEx implements HardwareDevice {

    public static final double kDefaultEncoderMultiplier = 1.0;

    /**
     * The motor that is paired with the encoder. This assumes that the encoder
     * is internal rather than external.
     */
    private MotorEx motor_w_encoder;

    /**
     * The total number of ticks that have accumulated before the last reset.
     */
    private double resetVal;

    /**
     * The multiplier for the encoder, which determines its direction.
     *
     * <p>By default, the multiplier is {@value kDefaultEncoderMultiplier}</p>
     */
    private double multiplier = kDefaultEncoderMultiplier;

    /**
     * The constructor for the extended-use encoder.
     *
     * @param motor The motor tied to said encoder.
     */
    public EncoderEx(MotorEx motor) {
        motor_w_encoder = motor;
    }

    /**
     * @return The current number of ticks reached by the output shaft since the last reset.
     */
    public double getCurrentTicks() {
        return multiplier * (motor_w_encoder.getCurrentPosition() - resetVal);
    }

    /**
     * Resets the encoder count to 0 while running.
     * This is the most useful feature of EncoderEx, as it allows the encoder to continue
     * running and resetting the return value without having to STOP_AND_RESET.
     */
    public void resetEncoderCount() {
        resetVal += getCurrentTicks();
    }

    /**
     * Inverts the multiplier. Note that the multiplier is
     * set to {@value #kDefaultEncoderMultiplier} by default.
     */
    public void invert() {
        multiplier *= -1;
    }

    /**
     * @return True if the encoder is reversed, meaning the multiplier is -1.
     */
    public boolean isReversed() {
        return multiplier < 0;
    }

    /**
     * @return The number of revolutions spun by the motor since the last reset.
     */
    public double getNumRevolutions() {
        return getCurrentTicks() / motor_w_encoder.COUNTS_PER_REV;
    }

    /**
     * Stops the motor and resets the encoder.
     */
    public void stopAndReset() {
        motor_w_encoder.set(0);
        resetEncoderCount();
    }

    /**
     * Disables the encoder.
     */
    @Override
    public void disable() {
        multiplier = 0;
        stopAndReset();
    }

    /**
     * @return The type of device.
     */
    @Override
    public String getDeviceType() {
        return "Extended Encoder";
    }

}
