package com.arcrobotics.ftclib.drivebase;

import com.arcrobotics.ftclib.controller.PController;
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
     * The PController for the angling mechanism.
     */
    private PController pController;

    /**
     * The current angle of the module.
     */
    private double angle;

    /**
     * Constructor for the module.
     *
     * @param turn  The motor that rotates the base.
     * @param drive The motor that drives the wheel.
     */
    public SwerveModule(Motor turn, Motor drive, double p) {
        turnMotor = turn;
        driveMotor = drive;

        pController = new PController(p);
        angle = 0;
    }

    /**
     * Drive the module using two input speeds, one that controls the translational
     * vector and the other that controls the rotational vector.
     *
     * @param speed The speed of the module.
     * @param angle The angle of the module.
     */
    public void driveModule(double speed, double angle) {
        driveMotor.set(speed);

        pController.pControl(turnMotor, angle, this.angle);
        this.angle = angle;
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
