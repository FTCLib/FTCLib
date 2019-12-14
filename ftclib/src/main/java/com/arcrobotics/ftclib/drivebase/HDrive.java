package com.arcrobotics.ftclib.drivebase;

import com.arcrobotics.ftclib.geometry.Vector2d;
import com.arcrobotics.ftclib.hardware.motors.Motor;

@Deprecated()
public class HDrive extends RobotDrive
{
    Motor[] motors;

    /**
     * Constructor for the H-Drive class, which requires at least three motors.
     *
     * @param m1    one of the necessary primary drive motors
     * @param m2    one of the necessary primary drive motors
     * @param slide the necessary slide motor for the use of h-drive
     * @param motor the rest of the motors, potentially the other motors if its 4wd
     */
    public HDrive(Motor m1, Motor m2, Motor slide, Motor... motor)
    {
        this.motors = new Motor[motor.length + 3];
        System.arraycopy(motor, 0, this.motors, 0, motor.length);
        this.motors[motor.length] = m1;
        this.motors[motor.length + 1] = m2;
        this.motors[motor.length + 2] = slide;
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
        vector = vector.rotateBy(heading);

        double theta = Math.atan2(ySpeed, xSpeed);

        double[] speeds = new double[4];

//        speeds[MotorType.kLeft.value] =
//        speeds[MotorType.kRight.value] =
//        speeds[MotorType.kSlide.value] =

        normalize(speeds);

        motors[MotorType.kLeft.value].set(speeds[MotorType.kLeft.value] * maxOutput);
        motors[MotorType.kRight.value].set(speeds[MotorType.kRight.value] * -maxOutput);
        motors[MotorType.kSlide.value].set(speeds[MotorType.kSlide.value] * maxOutput);
    }

    public void driveRobotCentric(double xSpeed, double ySpeed, double turn){
        driveFieldCentric(xSpeed, ySpeed, turn, 0.0);
    }
}
