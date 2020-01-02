package com.arcrobotics.ftclib.drivebase;

import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.hardware.motors.MotorGroup;

/**
 * A differential drive is one that has two motors or motor groups
 * on either side of the robot. Each side acts as a connected set.
 * You can see {@link MotorGroup} for more details as to how that might work.
 * There are two types of drive systems here. You can use tank and arcade.
 *
 * <p>
 *     Arcade drive use a y-value input from the controller and a value from
 *     the turn stick. We know that when the turn stick is pushed left, the right
 *     side should move forward and the left side should move backwards.
 *     Therefore, since pushing the turn stick to the left returns a negative value,
 *     it should be added to the left speed and subtracted from the right speed.
 *     <br><br />
 *     Tank drive uses a y-value input from the left and right sticks. The sticks
 *     control their respective side of the robot.
 * </p>
 */
public class DifferentialDrive extends RobotDrive {

    public static final double kDefaultRightSideMultiplier = -1.0;

    private Motor[] motors;
    private double rightSideMultiplier = kDefaultRightSideMultiplier;

    /**
     * Construct a DifferentialDrive.
     *
     * <p>
     *     To pass multiple motors per side, use a {@link MotorGroup}. If a motor needs to
     *     be inverted, do so before passing it in. By default, the right motor speed multiplier
     *     is {@value #kDefaultRightSideMultiplier}.
     * </p>
     *
     * @param myMotors The {@link Motor} objects in the differential drive. Must be in the order
     *                 of left, right.
     */
    public DifferentialDrive(Motor... myMotors) {
        motors = myMotors;
        setRightSideInverted(true);
    }

    /**
     * Construct a DifferentialDrive.
     *
     * <p>
     *     To pass multiple motors per side, use a {@link MotorGroup}. If a motor needs to
     *     be inverted, do so before passing it in.
     * </p>
     * @param autoInvert Whether or not to automatically invert right side
     * @param myMotors The {@link Motor} objects in the differential drive. Must be in the order
     *                 of left, right.
     */
    public DifferentialDrive(boolean autoInvert, Motor... myMotors) {
        motors = myMotors;
        setRightSideInverted(autoInvert);
    }


    /**
     * Checks if the right side motors are inverted.
     *
     * @return true if the multiplier for the right side is equal to -1.
     */
    public boolean isRightSideInverted() {
        return rightSideMultiplier == -1.0;
    }

    /**
     * Sets the right side inversion factor to the specified boolean.
     *
     * @param isInverted If true, sets the right side multiplier to -1 or 1 if false.
     */
    public void setRightSideInverted(boolean isInverted) {
        rightSideMultiplier = isInverted ? -1.0 : 1.0;
    }

    /**
     * Stop the motors.
     */
    @Override
    public void stopMotor() {
        for (Motor x : motors) {
            x.stopMotor();
        }
    }

    /**
     * Sets the range of the input, see RobotDrive for more info.
     *
     * @param min The minimum value of the range.
     * @param max The maximum value of the range.
     */
    public void setRange(double min, double max) {
        super.setRange(min, max);
    }

    /**
     * Sets the max speed of the drivebase, see RobotDrive for more info.
     *
     * @param value The maximum output speed.
     */
    public void setMaxSpeed(double value) {
        super.setMaxSpeed(value);
    }

    /**
     * Drives the robot using the arcade system.
     *
     * @param ySpeed    The input value that determines the vertical speed of the robot.
     * @param turnSpeed The input value that determines the rotational speed of the robot.
     */
    public void arcadeDrive(double ySpeed, double turnSpeed) {
        ySpeed = clipRange(ySpeed);
        turnSpeed = clipRange(turnSpeed);

        double[] wheelSpeeds = new double[2];
        wheelSpeeds[MotorType.kLeft.value] = ySpeed + turnSpeed;
        wheelSpeeds[MotorType.kRight.value] = ySpeed - turnSpeed;

        normalize(wheelSpeeds);

        motors[MotorType.kLeft.value].set(maxOutput * wheelSpeeds[0]);
        motors[MotorType.kRight.value].set(rightSideMultiplier * maxOutput * wheelSpeeds[1]);
    }

    /**
     * Drives the robot using the arcade system.
     *
     * @param ySpeed    The input value that determines the vertical speed of the robot.
     * @param turnSpeed The input value that determines the rotational speed of the robot.
     * @param squareInputs Square the value of the input to allow for finer control
     */
    public void arcadeDrive(double ySpeed, double turnSpeed, boolean squareInputs) {
        ySpeed = squareInputs ? clipRange(squareInput(ySpeed)) : clipRange(ySpeed);
        turnSpeed = squareInputs ? clipRange(squareInput(turnSpeed)) : clipRange(turnSpeed);

        arcadeDrive(ySpeed, turnSpeed);
    }


    /**
     * Drive the robot using the tank system.
     *
     * @param leftSpeed     The input value that determines the speed of the left side motors.
     * @param rightSpeed    The input value that determines the speed of the right side motors.
     */
    public void tankDrive(double leftSpeed, double rightSpeed) {
        leftSpeed = clipRange(leftSpeed);
        rightSpeed = clipRange(rightSpeed);

        double[] wheelSpeeds = new double[2];
        wheelSpeeds[MotorType.kLeft.value] = leftSpeed;
        wheelSpeeds[MotorType.kRight.value] = rightSpeed;

        normalize(wheelSpeeds);

        motors[MotorType.kLeft.value].set(wheelSpeeds[0] * maxOutput);
        motors[MotorType.kRight.value].set(wheelSpeeds[1] * -maxOutput);
    }

    /**
     * Drive the robot using the tank system.
     *
     * @param leftSpeed     The input value that determines the speed of the left side motors.
     * @param rightSpeed    The input value that determines the speed of the right side motors.
     * @param squareInputs Square the value of the input to allow for finer control
     */
    public void tankDrive(double leftSpeed, double rightSpeed, boolean squareInputs) {
        leftSpeed = squareInputs ? clipRange(squareInput(leftSpeed)) : clipRange(leftSpeed);
        rightSpeed = squareInputs ? clipRange(squareInput(rightSpeed)) : clipRange(rightSpeed);

        tankDrive(leftSpeed, rightSpeed);
    }


}
