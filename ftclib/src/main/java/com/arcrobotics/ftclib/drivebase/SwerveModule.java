package com.arcrobotics.ftclib.drivebase;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.arcrobotics.ftclib.geometry.Vector2d;
import com.arcrobotics.ftclib.hardware.Motor;

public class SwerveModule {

    /**
     * The motors in the swerve module. One spins the wheel, creating translation,
     * and the other turns the base, creating rotation.
     */
    private Motor turnMotor, driveMotor;

    /**
     * Constructor for the module.
     *
     * @param turn  The motor that rotates the base.
     * @param drive The motor that drives the wheel.
     */
    public SwerveModule(Motor turn, Motor drive) {
        turnMotor = turn;
        driveMotor = drive;


    }

    /**
     * Drive the module using two input speeds, one that controls the translational
     * vector and the other that controls the rotational vector.
     *
     * @param speed
     */
    public void driveModule(Vector2d speed) {

    }

    /**
     * Disables the module.
     */
    public void disable() {
        turnMotor.disable();
        driveMotor.disable();
    }

    /**
     * Stops the motors.
     */
    public void stopMotor() {
        turnMotor.stopMotor();
        driveMotor.stopMotor();
    }

}
