package com.arcrobotics.ftclib.hardware.motors;

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/**
 * An extended motor class that utilizes more features than the
 * regular motor.
 *
 * @author Jackson
 */
public class MotorEx extends Motor {

    /**
     * The motor for the MotorEx class.
     */
    public DcMotorEx motorEx;

    /**
     * Constructs the instance motor for the wrapper
     *
     * @param hMap the hardware map from the OpMode
     * @param id   the device id from the RC config
     */
    public MotorEx(@NonNull HardwareMap hMap, String id) {
        this(hMap, id, GoBILDA.NONE);
        ACHIEVABLE_MAX_TICKS_PER_SECOND = motorEx.getMotorType().getAchieveableMaxTicksPerSecond();
    }

    /**
     * Constructs the instance motor for the wrapper
     *
     * @param hMap        the hardware map from the OpMode
     * @param id          the device id from the RC config
     * @param gobildaType the type of gobilda 5202 series motor being used
     */
    public MotorEx(@NonNull HardwareMap hMap, String id, @NonNull GoBILDA gobildaType) {
        super(hMap, id, gobildaType);
        motorEx = (DcMotorEx) super.motor;
    }

    /**
     * Constructs an instance motor for the wrapper
     *
     * @param hMap the hardware map from the OpMode
     * @param id   the device id from the RC config
     * @param cpr  the counts per revolution of the motor
     * @param rpm  the revolutions per minute of the motor
     */
    public MotorEx(@NonNull HardwareMap hMap, String id, double cpr, double rpm) {
        super(hMap, id, cpr, rpm);
        motorEx = (DcMotorEx) super.motor;
    }

    @Override
    public void set(double output) {
        if (runmode == RunMode.VelocityControl) {
            double speed = bufferFraction * output * ACHIEVABLE_MAX_TICKS_PER_SECOND;
            double velocity = veloController.calculate(getCorrectedVelocity(), speed) + feedforward.calculate(speed, getAcceleration());
            motorEx.setPower(velocity / ACHIEVABLE_MAX_TICKS_PER_SECOND);
        } else if (runmode == RunMode.PositionControl) {
            double error = positionController.calculate(encoder.getPosition());
            motorEx.setPower(output * error);
        } else {
            motorEx.setPower(output);
        }
    }

    /**
     * @param velocity the velocity in ticks per second
     */
    public void setVelocity(double velocity) {
        set(velocity / ACHIEVABLE_MAX_TICKS_PER_SECOND);
    }

    /**
     * Sets the velocity of the motor to an angular rate
     *
     * @param velocity  the angular rate
     * @param angleUnit radians or degrees
     */
    public void setVelocity(double velocity, AngleUnit angleUnit) {
        setVelocity(getCPR() * AngleUnit.RADIANS.fromUnit(angleUnit, velocity) / (2 * Math.PI));
    }

    /**
     * @return the velocity of the motor in ticks per second
     */
    @Override
    public double getVelocity() {
        return motorEx.getVelocity();
    }

    /**
     * @return the acceleration of the motor in ticks per second squared
     */
    public double getAcceleration() {
        return encoder.getAcceleration();
    }

    @Override
    public String getDeviceType() {
        return "Extended " + super.getDeviceType();
    }

}