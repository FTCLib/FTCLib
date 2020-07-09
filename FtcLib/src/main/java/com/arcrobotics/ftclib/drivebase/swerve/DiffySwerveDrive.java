package com.arcrobotics.ftclib.drivebase.swerve;

import com.arcrobotics.ftclib.drivebase.RobotDrive;
import com.arcrobotics.ftclib.geometry.Vector2d;

public class DiffySwerveDrive extends RobotDrive {

    private DiffySwerveModule left, right;

    public DiffySwerveDrive(DiffySwerveModule left, DiffySwerveModule right) {
        this(left, right, true);
    }

    public DiffySwerveDrive(DiffySwerveModule left, DiffySwerveModule right, boolean rightInversion) {
        this.left = left;
        this.right = right;

        setRightSideInverted(rightInversion);
    }

    public void setRightSideInverted(boolean isInverted) {
        right.setInverted(isInverted);
    }

    public void drive(double leftX, double leftY, double rightX, double rightY) {
        drive(new Vector2d(leftX, leftY), new Vector2d(rightX, rightY));
    }

    /**
     * Angles must be rotated in order to
     * account for the way the projections work. If the joystick
     * is pushed forward, then we want the robot to move forward
     * and not rotate at all. This means that we want strict translation.
     * This means we want a vector of (mag, 0) which corresponds to
     * forward motion. Since the joystick is (0, mag), we want to rotate it
     * by 90 degrees clockwise.
     */
    public void drive(Vector2d leftPower, Vector2d rightPower) {
        leftPower = leftPower.rotateBy(-90);
        rightPower = rightPower.rotateBy(-90);

        left.driveModule(leftPower);
        right.driveModule(rightPower);
    }

    @Override
    public void stopMotor() {
        left.stopMotor();
        right.stopMotor();
    }

    public void disable() {
        left.disable();
        right.disable();
    }

}
