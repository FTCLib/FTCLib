package com.arcrobotics.ftclib.hardware.motors;

import android.support.annotation.NonNull;

import com.arcrobotics.ftclib.controller.PController;
import com.arcrobotics.ftclib.controller.PIDController;
import com.arcrobotics.ftclib.controller.wpilibcontroller.SimpleMotorFeedforward;
import com.arcrobotics.ftclib.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.function.Supplier;

/**
 * This is the common wrapper for the {@link DcMotor} object in the
 * FTC SDK.
 *
 * @author Jackson
 */
public class Motor implements HardwareDevice {

    public class Encoder {

        private Supplier<Integer> m_position;
        private int resetVal;

        /**
         * The encoder object for the motor.
         *
         * @param position  the position supplier which just points to the
         *                  current position of the motor in ticks
         */
        public Encoder(Supplier<Integer> position) {
            m_position = position;
            resetVal = 0;
        }

        /**
         * @return  the current position of the internal encoder
         */
        public int getPosition() {
            return m_position.get() - resetVal;
        }

        /**
         * Resets the encoder without having to stop the motor.
         */
        public void reset() {
            resetVal = getPosition();
        }

        /**
         * @return  the number of revolutions turned by the motor
         */
        public double getRevolutions() {
            return getPosition() / getCPR();
        }

    }

    /**
     * The RunMode of the motor.
     */
    public enum RunMode {
        VelocityControl, PositionControl, RawPower
    }

    public DcMotor motor;
    public Encoder encoder;

    /**
     * The runmode of the motor
     */
    protected RunMode runmode;

    /**
     * The achievable ticks per second velocity of the motor
     */
    public double ACHIEVABLE_MAX_TICKS_PER_SECOND;

    private PIDController veloController;
    private PController positionController;

    private SimpleMotorFeedforward feedforward;

    private int targetPosition;

    public Motor() {}

    /**
     * Constructs the instance motor for the
     * @param hMap  the hardware map from the OpMode
     * @param id    the device id from the RC config
     */
    public Motor(@NonNull HardwareMap hMap, String id) {
        motor = hMap.get(DcMotor.class, id);
        runmode = RunMode.RawPower;
        ACHIEVABLE_MAX_TICKS_PER_SECOND = motor.getMotorType().getAchieveableMaxTicksPerSecond();
        veloController = new PIDController(1,0,0);
        positionController = new PController(1);
        feedforward = new SimpleMotorFeedforward(0, 1, 0);
        encoder = new Encoder(motor::getCurrentPosition);
    }

    /**
     * Common method for setting the speed of a motor.
     *
     * @param output The percentage of power to set. Value should be between -1.0 and 1.0.
     */
    public void set(double output) {
        if (runmode == RunMode.VelocityControl) {
            double speed = output * ACHIEVABLE_MAX_TICKS_PER_SECOND;
            double velocity = veloController.calculate(getVelocity(), speed) + feedforward.calculate(speed);
            motor.setPower(velocity / ACHIEVABLE_MAX_TICKS_PER_SECOND);
        } else if (runmode == RunMode.PositionControl) {
            double error = positionController.calculate(encoder.getPosition(), targetPosition);
            motor.setPower(output * error);
        } else {
            motor.setPower(output);
        }
    }

    /**
     * Resets the encoder.
     */
    public void resetEncoder() {
        encoder.reset();
    }

    /**
     * @return  the current position of the motor in ticks
     */
    public int getCurrentPosition() {
        return encoder.getPosition();
    }

    /**
     * @return the counts per revolution of the motor
     */
    public double getCPR() {
        return motor.getMotorType().getTicksPerRev();
    }

    /**
     * @return the max RPM of the motor
     */
    public double getMaxRPM() {
        return motor.getMotorType().getMaxRPM();
    }

    /**
     * Sets the {@link RunMode} of the motor
     *
     * @param runmode the desired runmode
     */
    public void setRunMode(RunMode runmode) {
        this.runmode = runmode;
        veloController.reset();
        positionController.reset();
    }

    protected double getVelocity() {
        return get() * ACHIEVABLE_MAX_TICKS_PER_SECOND;
    }

    /**
     * Common method for getting the current set speed of a motor.
     *
     * @return The current set speed. Value is between -1.0 and 1.0.
     */
    public double get() {
        return motor.getPower();
    }

    /**
     * Sets the target position for the motor to the desired target.
     * Once {@link #set(double)} is called, the motor will attempt to move in the direction
     * of said target.
     *
     * @param target
     */
    public void setTargetPosition(int target) {
        targetPosition = target;
    }

    /**
     * Common method for inverting direction of a motor.
     *
     * @param isInverted The state of inversion true is inverted.
     */
    public void setInverted(boolean isInverted) {
        motor.setDirection(isInverted ? DcMotor.Direction.REVERSE : DcMotor.Direction.FORWARD);
    }

    /**
     * Common method for returning if a motor is in the inverted state or not.
     *
     * @return isInverted The state of the inversion true is inverted.
     */
    public boolean getInverted() {
        return DcMotor.Direction.REVERSE == motor.getDirection();
    }

    /**
     * Set the velocity pid coefficients for the motor.
     *
     * @param kp    the proportional gain
     * @param ki    the integral gain
     * @param kd    the derivative gain
     */
    public void setVeloCoefficients(double kp, double ki, double kd) {
        veloController.setPIDF(kp, ki, kd, 0);
    }

    /**
     * Set the feedforward coefficients for the motor.
     *
     * @param ks    the static gain
     * @param kv    the velocity gain
     */
    public void setFeedforwardCoefficients(double ks, double kv) {
        feedforward = new SimpleMotorFeedforward(ks, kv);
    }

    /**
     * Set the proportional gain for the position controller.
     *
     * @param kp    the proportional gain
     */
    public void setPositionCoefficient(double kp) {
        positionController.setP(kp);
    }

    /**
     * Disable the motor.
     */
    @Override
    public void disable() {
        motor.close();
    }

    @Override
    public String getDeviceType() {
        return "Motor " + motor.getDeviceName() + " from " + motor.getManufacturer()
                + " in port " + motor.getPortNumber();
    }

    /**
     * Stops motor movement. Motor can be moved again by calling set without having to re-enable the
     * motor.
     */
    public void stopMotor() {
        set(0);
    }

}
