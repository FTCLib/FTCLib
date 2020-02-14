package com.arcrobotics.ftclib.kinematics;

import com.arcrobotics.ftclib.drivebase.DifferentialDrive;
import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Transform2d;
import com.arcrobotics.ftclib.geometry.Translation2d;

/**
 * The classfile that performs odometry calculations for a differential drivetrain.
 * For more information on the differential drivebase, see {@link DifferentialDrive}.
 */
public class DifferentialOdometry {

    /**
     * The {@link Pose2d} of the robot.
     */
    public Pose2d robotPose;

    /**
     * The x value for the position of the robot.
     */
    public double odoX;

    /**
     * The y value for the position of the robot.
     */
    public double odoY;


    /**
     * The distance between the left and right main wheels on the robot.
     * This defaults to 18.
     */
    public double trackWidth;

    /**
     * Empty constructor. Position is defaulted to (0, 0, 0) and track width is 18 inches.
     */
    public DifferentialOdometry() {
        this(18);
    }

    /**
     * The constructor that specifies the track width of the robot but defaults
     * the position to (0, 0, 0).
     *
     * @param trackWidth The track width of the robot in inches.
     */
    public DifferentialOdometry(double trackWidth) {
        this(new Pose2d(new Translation2d(0, 0), new Rotation2d(0)), trackWidth);
    }

    /**
     * The constructor that specifies the starting position and the track width.
     *
     * @param pose          the starting position of the robot
     * @param trackWidth    the track width of the robot in inches
     */
    public DifferentialOdometry(Pose2d pose, double trackWidth) {
        this.trackWidth = trackWidth;
        robotPose = pose;
    }

    /**
     * Updates the position of the robot.
     *
     * @param newPose   the new {@link Pose2d}
     */
    public void updatePose(Pose2d newPose) {
        robotPose = newPose;
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

    /**
     * Updates the values for odoX and odoY by adding the specified amounts.
     *
     * @param xVal the additive x-value
     * @param yVal the additive y-value
     */
    public void updateOdometryCounts(double xVal, double yVal) {
        odoX += xVal;
        odoY += yVal;
    }

    /**
     * The update method that updates the robot's position over a relatively small amount of time.
     * The update frequency can range in a value of Hz. However many updates that are called per
     * second is the representation of the feedback frequency in Hertz (1/s). If we have
     * a frequency of 60 Hz, then the update method is called 60 times in 1 second.
     * If you have no heading, pass in a value of 0 for each update. If you have no horizontal odometers,
     * pass in a value of 0 for horizontalOdoInches.
     *
     * @param heading       the current heading of the robot, which might be de-synced
     *                      with the heading in the robot position.
     * @param leftInches    the number of inches travelled by the left side of the robot.
     * @param rightInches   the number of inches travelled by the right side of the robot.
     */
    public void update(double heading, double leftInches, double rightInches) {
        double centralEncoderVal = (leftInches + rightInches) / 2;

        double phi = (rightInches - leftInches) / trackWidth;
        double theta = robotPose.getHeading();
        double deltaTheta = (heading != 0) ? heading - theta : phi;

        double deltaX = centralEncoderVal * Math.cos(theta + deltaTheta / 2);
        double deltaY = centralEncoderVal * Math.sin(theta + deltaTheta / 2);

        deltaX -= centralEncoderVal * Math.sin(theta + deltaTheta / 2);
        deltaY += centralEncoderVal * Math.cos(theta + deltaTheta / 2);

        updateOdometryCounts(deltaX, deltaY);
        rotatePose(deltaTheta);

        updatePose(new Pose2d(odoX, odoY, new Rotation2d(robotPose.getHeading())));
    }

}