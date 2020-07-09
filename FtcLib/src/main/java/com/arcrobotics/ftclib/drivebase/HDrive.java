package com.arcrobotics.ftclib.drivebase;

import com.arcrobotics.ftclib.geometry.Vector2d;
import com.arcrobotics.ftclib.hardware.motors.Motor;

/**
 * Holonomic drivebase
 */
public class HDrive extends RobotDrive
{
    Motor[] motors;

    public static final double kDefaultRightMotorAngle = Math.PI / 3;
    public static final double kDefaultLeftMotorAngle = 2 * Math.PI / 3;
    public static final double kDefaultSlideMotorAngle = 3 * Math.PI / 2;

    private double rightMotorAngle = kDefaultRightMotorAngle;
    private double leftMotorAngle = kDefaultLeftMotorAngle;
    private double slideMotorAngle = kDefaultSlideMotorAngle;

    /**
     * Constructor for the H-Drive class, which requires at least three motors.
     *
     * @param mLeft     one of the necessary primary drive motors
     * @param mRight    one of the necessary primary drive motors
     * @param slide     the necessary slide motor for the use of h-drive
     */
    public HDrive(Motor mLeft, Motor mRight, Motor slide) {
        motors = new Motor[3];
        motors[MotorType.kLeft.value] = mLeft;
        motors[MotorType.kRight.value] = mRight;
        motors[MotorType.kSlide.value] = slide;
    }

    /**
     * The constructor that includes the angles of the motors.
     *
     * <p>
     *     The default angles are {@value #kDefaultRightMotorAngle},
     *     {@value #kDefaultLeftMotorAngle}, {@value #kDefaultSlideMotorAngle}.
     * </p>
     *
     * @param mLeft             one of the necessary primary drive motors
     * @param mRight            one of the necessary primary drive motors
     * @param slide             the necessary slide motor for the use of h-drive
     * @param leftMotorAngle    the angle of the left motor in radians
     * @param rightMotorAngle   the angle of the right motor in radians
     * @param slideMotorAngle   the angle of the slide motor in radians
     */
    public HDrive(Motor mLeft, Motor mRight, Motor slide, double leftMotorAngle,
                  double rightMotorAngle, double slideMotorAngle) {
        motors = new Motor[3];
        motors[0] = mLeft;
        motors[1] = mRight;
        motors[2] = slide;

        this.leftMotorAngle = leftMotorAngle;
        this.rightMotorAngle = rightMotorAngle;
        this.slideMotorAngle = slideMotorAngle;
    }

    /**
     * Sets up the constructor for the holonomic drive.
     *
     * @param myMotors The motors in order of:
     *                 frontLeft, frontRight, backLeft, backRight.
     *                 Do not input in any other order.
     */
    public HDrive(Motor... myMotors) {
        motors = myMotors;
    }

    /**
     * Sets the range of the input, see {@link RobotDrive} for more info.
     *
     * @param min The minimum value of the range.
     * @param max The maximum value of the range.
     */
    public void setRange(double min, double max) {
        super.setRange(min, max);
    }

    /**
     * Sets the max speed of the drivebase, see {@link RobotDrive} for more info.
     *
     * @param value The maximum output speed.
     */
    public void setMaxSpeed(double value) {
        super.setMaxSpeed(value);
    }

    public void stopMotor(){
        for (Motor x : motors) {
            x.stopMotor();
        }
    }

    public void driveFieldCentric(double xSpeed, double ySpeed, double turn, double heading)
    {
        xSpeed = clipRange(xSpeed);
        ySpeed = clipRange(ySpeed);
        turn = clipRange(turn);

        Vector2d vector = new Vector2d(xSpeed, ySpeed);
        vector = vector.rotateBy(-heading);

        double theta = Math.atan2(ySpeed, xSpeed);

        double[] speeds = new double[motors.length];

        if (speeds.length == 3) {
            Vector2d leftVec = new Vector2d(Math.cos(leftMotorAngle), Math.sin(leftMotorAngle));
            Vector2d rightVec = new Vector2d(Math.cos(rightMotorAngle), Math.sin(rightMotorAngle));
            Vector2d slideVec = new Vector2d(Math.cos(slideMotorAngle), Math.sin(slideMotorAngle));

            speeds[MotorType.kLeft.value] = vector.scalarProject(leftVec) + turn;
            speeds[MotorType.kRight.value] = vector.scalarProject(rightVec) + turn;
            speeds[MotorType.kSlide.value] = vector.scalarProject(slideVec) + turn;

            normalize(speeds);

            motors[MotorType.kLeft.value].set(speeds[MotorType.kRight.value] * maxOutput);
            motors[MotorType.kRight.value].set(speeds[MotorType.kLeft.value] * maxOutput);
            motors[MotorType.kSlide.value].set(speeds[MotorType.kSlide.value] * maxOutput);
        }
        // this looks similar to mecanum because mecanum is a four wheel holonomic drivebase
        else {
            speeds[MotorType.kFrontLeft.value] =
                    vector.magnitude() * Math.sin(theta + Math.PI / 4) + turn;
            speeds[MotorType.kFrontRight.value] =
                    vector.magnitude() * Math.sin(theta - Math.PI / 4) - turn;
            speeds[MotorType.kBackLeft.value] =
                    vector.magnitude() * Math.sin(theta - Math.PI / 4) + turn;
            speeds[MotorType.kBackRight.value] =
                    vector.magnitude() * Math.sin(theta + Math.PI / 4) - turn;

            normalize(speeds);

            motors[MotorType.kFrontLeft.value]
                    .set(speeds[MotorType.kFrontLeft.value] * maxOutput);
            motors[MotorType.kFrontRight.value]
                    .set(speeds[MotorType.kFrontRight.value] * -maxOutput);
            motors[MotorType.kBackLeft.value]
                    .set(speeds[MotorType.kBackLeft.value] * maxOutput);
            motors[MotorType.kBackRight.value]
                    .set(speeds[MotorType.kBackRight.value] * -maxOutput);
        }

    }

    public void driveRobotCentric(double xSpeed, double ySpeed, double turn){
        driveFieldCentric(xSpeed, ySpeed, turn, 0.0);
    }
}
