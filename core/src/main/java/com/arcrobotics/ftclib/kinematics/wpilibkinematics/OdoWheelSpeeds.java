package com.arcrobotics.ftclib.kinematics.wpilibkinematics;

public class OdoWheelSpeeds {
    /**
     * Speed of the left side of the robot.
     */
    public double leftMetersPerSecond;

    /**
     * Speed of the right side of the robot.
     */
    public double rightMetersPerSecond;

    /**
     * Speed of strafing
     */
    public double centerMetersPerSecond;

    /**
     * Constructs a OdoWheelSpeeds with zeros for left and right speeds.
     */
    public OdoWheelSpeeds() {
    }

    /**
     * Constructs a DifferentialDriveWheelSpeeds.
     *
     * @param leftMetersPerSecond  The left speed.
     * @param rightMetersPerSecond The right speed.
     * @param centerMetersPerSecond The strafe speed.
     */
    public OdoWheelSpeeds(double leftMetersPerSecond, double rightMetersPerSecond, double centerMetersPerSecond) {
        this.leftMetersPerSecond = leftMetersPerSecond;
        this.rightMetersPerSecond = rightMetersPerSecond;
        this.centerMetersPerSecond = centerMetersPerSecond;
    }

    /**
     * Normalizes the wheel speeds using some max attainable speed. Sometimes,
     * after inverse kinematics, the requested speed from a/several modules may be
     * above the max attainable speed for the driving motor on that module. To fix
     * this issue, one can "normalize" all the wheel speeds to make sure that all
     * requested module speeds are below the absolute threshold, while maintaining
     * the ratio of speeds between modules.
     *
     * @param attainableMaxSpeedMetersPerSecond The absolute max speed that a wheel can reach.
     */
    public void normalize(double attainableMaxSpeedMetersPerSecond) {
        double realMaxSpeed = Math.max(Math.abs(leftMetersPerSecond), Math.abs(rightMetersPerSecond));

        if (realMaxSpeed > attainableMaxSpeedMetersPerSecond) {
            leftMetersPerSecond = leftMetersPerSecond / realMaxSpeed
                    * attainableMaxSpeedMetersPerSecond;
            rightMetersPerSecond = rightMetersPerSecond / realMaxSpeed
                    * attainableMaxSpeedMetersPerSecond;
            centerMetersPerSecond = centerMetersPerSecond / realMaxSpeed * attainableMaxSpeedMetersPerSecond;
        }
    }

    @Override
    public String toString() {
        return String.format("OdoWheelSpeeds(Left: %.2f m/s, Right: %.2f m/s, Strafe: %.sf m/s)",
                leftMetersPerSecond, rightMetersPerSecond, centerMetersPerSecond);
    }
}
