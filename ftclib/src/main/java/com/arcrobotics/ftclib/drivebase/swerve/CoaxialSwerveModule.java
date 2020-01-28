package com.arcrobotics.ftclib.drivebase.swerve;

import com.arcrobotics.ftclib.controller.PController;
import com.arcrobotics.ftclib.hardware.motors.Motor;

/**
 * The module for a swerve drive. Each module contains two motors: one for turning,
 * and the other for driving. The combination of these two factors makes for a wide degree of motion
 * using three degrees of freedom with traction, making it a more effective version of a mecanum drive.
 */
public class CoaxialSwerveModule {

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
    public CoaxialSwerveModule(Motor turn, Motor drive, double p) {
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
