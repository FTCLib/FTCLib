package com.arcrobotics.ftclib.kinematics;

import com.arcrobotics.ftclib.geometry.Pose2d;

public abstract class Odometry {

    /**
     * The {@link Pose2d} of the robot.
     */
    protected Pose2d robotPose;

    /**
     * The trackwidth of the odometers
     */
    protected double trackWidth;

    public Odometry(Pose2d robotPose) {
        this(robotPose, 18);
    }

    public Odometry(Pose2d robotPose, double trackWidth) {
        this.robotPose = robotPose;
        this.trackWidth = trackWidth;
    }

    /**
     * Updates the position of the robot.
     */
    public abstract void updatePose(Pose2d newPose);

    /**
     * Uses suppliers to update the position of the robot
     */
    public abstract void updatePose();

    /**
     * Returns the Pose2d object that represents the current robot position
     *
     * @return The robot pose
     */
    public Pose2d getPose() {
        return robotPose;
    }

    /**
     * Rotates the position of the robot by a given angle
     *
     * @param byAngle the angle to be rotated by, preferably in radians
     */
    public void rotatePose(double byAngle) {
        robotPose = robotPose.rotate(byAngle);
    }

}
