package com.arcrobotics.ftclib.kinematics;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;

public abstract class Odometry {


    /**
     * The {@link Pose2d} of the robot.
     */
    Pose2d robotPose;


    /**
     * The trackwidth of the odometers
     */
    double trackWidth;


    Odometry(Pose2d robotPose){
        this(robotPose, 18);
    }

    Odometry(Pose2d robotPose, double trackWidth){
        this.robotPose = robotPose;
        this.trackWidth = trackWidth;
    }


    /**
     * Sets the robot pose to the given pose
     * @param newPose The given pose
     */
    abstract void updatePose(Pose2d newPose);


    /**
     * Returns the Pose2d object that represents the current robot position
     * @return The robot pose
     */
    public Pose2d getPose(){
        return robotPose;
    }


    /**
     * Rotates the heading by the specified value.
     *
     * @param deltaTheta the difference between the current heading and
     *                   the previous heading. Rotates it CCW.
     */
    public void rotatePose(double deltaTheta) {
        updatePose(robotPose.rotate(deltaTheta));
    }








}
