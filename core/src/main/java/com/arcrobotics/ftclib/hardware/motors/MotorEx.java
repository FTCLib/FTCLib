package com.arcrobotics.ftclib.hardware.motors;

import androidx.annotation.NonNull;

import com.arcrobotics.ftclib.controller.PController;
import com.arcrobotics.ftclib.controller.PIDController;
import com.arcrobotics.ftclib.controller.wpilibcontroller.SimpleMotorFeedforward;
import com.qualcomm.robotcore.hardware.DcMotor;
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
        motorEx = hMap.get(DcMotorEx.class, id);
        runmode = RunMode.RawPower;
        type = gobildaType;
        ACHIEVABLE_MAX_TICKS_PER_SECOND = gobildaType.getAchievableMaxTicksPerSecond();
        veloController = new PIDController(1,0,0);
        positionController = new PController(1);
        feedforward = new SimpleMotorFeedforward(0, 1, 0);
        encoder = new Encoder(motorEx::getCurrentPosition);
    }

    /**
     * Constructs an instance motor for the wrapper
     *
     * @param hMap      the hardware map from the OpMode
     * @param id        the device id from the RC config
     * @param cpr       the counts per revolution of the motor
     * @param rpm       the revolutions per minute of the motor
     */
    public MotorEx(@NonNull HardwareMap hMap, String id, double cpr, double rpm) {
        motor = hMap.get(DcMotorEx.class, id);
        runmode = RunMode.RawPower;
        type = GoBILDA.NONE;
        ACHIEVABLE_MAX_TICKS_PER_SECOND = cpr * rpm / 60;
        veloController = new PIDController(1,0,0);
        positionController = new PController(1);
        feedforward = new SimpleMotorFeedforward(0, 1, 0);
        encoder = new Encoder(motor::getCurrentPosition);
    }

    @Override
    public void set(double output) {
        if (runmode == RunMode.VelocityControl) {
            double speed = output * ACHIEVABLE_MAX_TICKS_PER_SECOND;
            double velocity = veloController.calculate(getVelocity(), speed) + feedforward.calculate(speed);
            motorEx.setVelocity(velocity);
        } else if (runmode == RunMode.PositionControl) {
            double error = positionController.calculate(encoder.getPosition());
            motorEx.setPower(output * error);
        } else {
            motorEx.setPower(output);
        }
    }

    public void setVelocity(double velocity) {
        motorEx.setVelocity(velocity);
    }

    public void setVelocity(double velocity, AngleUnit angleUnit) {
        motorEx.setVelocity(velocity, angleUnit);
    }

    @Override
    public double getVelocity() {
        return encoder.getCorrectedVelocity();
    }

    @Override
    public String getDeviceType() {
        return "Extended " + super.getDeviceType();
    }

}