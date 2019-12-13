package com.arcrobotics.ftclib.drivebase;

import com.arcrobotics.ftclib.geometry.Vector2d;
import com.arcrobotics.ftclib.hardware.motors.Motor;

/**
 * This is a classfile representing the kinematics of a mecanum drivetrain
 * and controls their speed. The drive methods {@link #driveRobotCentric(double, double, double)}
 * and {@link #driveFieldCentric(double, double, double, double)} are meant to be put inside
 * of a loop. You can call them in {@code void loop()} in an OpMode and within
 * a {@code while (!isStopRequested() && opModeIsActive())} loop in the
 * {@code runOpMode()} method in LinearOpMode.
 *
 * For the derivation of mecanum kinematics, please watch this video:
 * https://www.youtube.com/watch?v=8rhAkjViHEQ.
 */
public class MecanumDrive extends RobotDrive {

    Motor[] motors;

    /**
     * Sets up the constructor for the mecanum drive.
     *
     * @param myMotors The motors in order of:
     *                 frontLeft, frontRight, backLeft, backRight.
     *                 Do not input in any other order.
     */
    public MecanumDrive(Motor... myMotors) {
        motors = myMotors;
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
     * Stop the motors.
     */
    @Override
    public void stopMotor() {
        for (Motor x : motors) {
            x.stopMotor();
        }
    }


    /**
     * Drives the robot from the perspective of the robot itself rather than that
     * of the driver.
     *
     * @param xSpeed    the horizontal speed of the robot, derived from input
     * @param ySpeed    the vertical speed of the robot, derived from input
     * @param turnSpeed the turn speed of the robot, derived from input
     */
    public void driveRobotCentric(double xSpeed, double ySpeed, double turnSpeed) {
        driveFieldCentric(xSpeed, ySpeed, turnSpeed, 0.0);
    }

    /**
     * Drives the robot from the perspective of the driver. No matter the orientation of the
     * robot, pushing forward on the drive stick will always drive the robot away
     * from the driver.
     *
     * @param xSpeed    the horizontal speed of the robot, derived from input
     * @param ySpeed    the vertical speed of the robot, derived from input
     * @param turnSpeed the turn speed of the robot, derived from input
     * @param squareInputs Square the value of the input to allow for finer control
     */
    public void driveRobotCentric(double xSpeed, double ySpeed, double turnSpeed, boolean squareInputs) {
        xSpeed = squareInputs ? clipRange(squareInput(xSpeed)) : clipRange(xSpeed);
        ySpeed = squareInputs ? clipRange(squareInput(ySpeed)) : clipRange(ySpeed);
        turnSpeed = squareInputs ? clipRange(squareInput(turnSpeed)) : clipRange(turnSpeed);

        driveRobotCentric(xSpeed, ySpeed, turnSpeed);
    }

    /**
     * Drives the robot from the perspective of the driver. No matter the orientation of the
     * robot, pushing forward on the drive stick will always drive the robot away
     * from the driver.
     *
     * @param xSpeed    the horizontal speed of the robot, derived from input
     * @param ySpeed    the vertical speed of the robot, derived from input
     * @param turnSpeed the turn speed of the robot, derived from input
     * @param gyroAngle the heading of the robot, derived from the gyro
     */
    public void driveFieldCentric(double xSpeed, double ySpeed,
                                  double turnSpeed, double gyroAngle) {
        xSpeed = clipRange(xSpeed);
        ySpeed = clipRange(ySpeed);
        turnSpeed = clipRange(turnSpeed);

        Vector2d input = new Vector2d(xSpeed, ySpeed);
        input = input.rotateBy(-gyroAngle);

        double theta = Math.atan2(ySpeed, xSpeed);

        double[] wheelSpeeds = new double[4];
        wheelSpeeds[MotorType.kFrontLeft.value] =
                input.magnitude() * Math.sin(theta + Math.PI / 4) + turnSpeed;
        wheelSpeeds[MotorType.kFrontRight.value] =
                input.magnitude() * Math.sin(theta - Math.PI / 4) - turnSpeed;
        wheelSpeeds[MotorType.kBackLeft.value] =
                input.magnitude() * Math.sin(theta - Math.PI / 4) + turnSpeed;
        wheelSpeeds[MotorType.kBackRight.value] =
                input.magnitude() * Math.sin(theta + Math.PI / 4) - turnSpeed;

        normalize(wheelSpeeds);

        motors[MotorType.kFrontLeft.value]
                .set(wheelSpeeds[MotorType.kFrontLeft.value] * maxOutput);
        motors[MotorType.kFrontRight.value]
                .set(wheelSpeeds[MotorType.kFrontRight.value] * -maxOutput);
        motors[MotorType.kBackLeft.value]
                .set(wheelSpeeds[MotorType.kBackLeft.value] * maxOutput);
        motors[MotorType.kBackRight.value]
                .set(wheelSpeeds[MotorType.kBackRight.value] * -maxOutput);
    }

    /**
     * Drives the robot from the perspective of the driver. No matter the orientation of the
     * robot, pushing forward on the drive stick will always drive the robot away
     * from the driver.
     *
     * @param xSpeed    the horizontal speed of the robot, derived from input
     * @param ySpeed    the vertical speed of the robot, derived from input
     * @param turnSpeed the turn speed of the robot, derived from input
     * @param gyroAngle the heading of the robot, derived from the gyro
     * @param squareInputs Square the value of the input to allow for finer control
     */
    public void driveFieldCentric(double xSpeed, double ySpeed, double turnSpeed, double gyroAngle, boolean squareInputs) {
        xSpeed = squareInputs ? clipRange(squareInput(xSpeed)) : clipRange(xSpeed);
        ySpeed = squareInputs ? clipRange(squareInput(ySpeed)) : clipRange(ySpeed);
        turnSpeed = squareInputs ? clipRange(squareInput(turnSpeed)) : clipRange(turnSpeed);

        driveFieldCentric(xSpeed, ySpeed, turnSpeed, gyroAngle);
    }

}