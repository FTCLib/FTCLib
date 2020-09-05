package com.arcrobotics.ftclib.hardware.motors;

import com.arcrobotics.ftclib.hardware.HardwareDevice;

/**
 * An extended encoder object. EncoderEx functions like a normal internal encoder
 * but has more methods and customization than a regular encoder.
 *
 * <p>
 *     Uses a {@link MotorEx} object for encoder values. This means the encoder is plugged
 *     into a motor encoder port on the REV Hub and paired to a motor object. Use
 *     {@link com.arcrobotics.ftclib.hardware.ExternalEncoder} for an external encoder i.e.
 *     REV through-bore.
 * </p>
 *
 * @author Jackson
 */
public class EncoderEx implements HardwareDevice {

    public static final int kDefaultEncoderMultiplier = 1;

    /**
     * The total number of ticks that have accumulated before the last reset.
     */
    private int resetVal;

    /**
     * The current number of ticks
     */
    private int ticks;

    /**
     * The multiplier for the encoder, which determines its direction.
     *
     * <p>By default, the multiplier is {@value kDefaultEncoderMultiplier}</p>
     */
    private int multiplier = kDefaultEncoderMultiplier;

    /**
     * @return The current number of ticks reached by the output shaft since the last reset.
     */
    public int getCurrentTicks() {
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
     * Sets the tolerance for the position control
     *
     * @param tolerance the desired tolerance
     */
    public void setPositionTolerance(double tolerance) {
        motor_w_encoder.positionController.setTolerance(tolerance);
    }

    /**
     * Runs the motor to a chosen position in space.
     *
     * @param target the desired tick reading of the motor
     */
    public void runToPosition(int target) {
        motor_w_encoder.pidWrite(
                motor_w_encoder.positionController.calculate(getCurrentTicks(), target)
        );
    }

    /**
     * Runs the motor to a chosen position in space.
     *
     * @param target the desired tick reading of the motor
     * @param speed the defined speed for the motor from 0 to 1
     */
    public void runToPosition(int target, double speed) {
        motor_w_encoder.pidWrite(
                speed * motor_w_encoder.positionController.calculate(getCurrentTicks(), target)
        );
    }

    /**
     * @return true if the motor has reached its setpoint target from
     * {@link #runToPosition(int)}.
     */
    public boolean reachedTarget() {
        return motor_w_encoder.positionController.atSetPoint();
    }

    /**
     * @return The type of device.
     */
    @Override
    public String getDeviceType() {
        return "Extended Encoder";
    }

}
