package com.arcrobotics.ftclib.kinematics;

import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Translation2d;

/**
 * The classfile that performs the calculations for the robot's odometry,
 * which is used for tracking position. This is specifically for a mecanum drivebase,
 * which has omnidirectional translation and rotational movement.
 * More information on the mecanum system can be found {@link MecanumDrive}.
 */
public class HolonomicOdometry {

    /**
     * The {@link Pose2d} of the robot.
     */
    public Pose2d robotPos;

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
    public HolonomicOdometry() {
        this(18);
    }

    /**
     * The constructor that specifies the track width of the robot but defaults
     * the position to (0, 0, 0).
     *
     * @param trackWidth The track width of the robot in inches.
     */
    public HolonomicOdometry(double trackWidth) {

        this(new Pose2d(new Translation2d(0, 0), new Rotation2d(0)), trackWidth);
    }

    /**
     * The constructor that specifies the starting position and the track width.
     *
     * @param pose          the starting position of the robot
     * @param trackWidth    the track width of the robot in inches
     */
    public HolonomicOdometry(Pose2d pose, double trackWidth) {
        robotPos = pose;
        this.trackWidth = trackWidth;
    }

    /**
     * Updates the position of the robot.
     *
     * @param newPose   the new {@link Pose2d}
     */
    public void updatePose(Pose2d newPose) {
        robotPos = newPose;
    }

    /**
     * Rotates the heading by the specified value.
     *
     * @param deltaTheta the difference between the current heading and
     *                   the previous heading. Rotates it CCW.
     */
    public void rotatePose(double deltaTheta) {
        robotPos = robotPos.rotate(deltaTheta);
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
     * The method that updates the curving values for the x and y coordinates of the
     * robot. This takes into account the curvature of the robot's motion.
     *
     * @param deltaTheta            the change in the robot's orientation
     * @param parallelEncoderVals   the value of the encoders for the parallel odometers
     */
    public void updateCurve(double deltaTheta, double... parallelEncoderVals) {
        double centralEncoderVal = 0;
        for (double val : parallelEncoderVals) {
            centralEncoderVal += val / parallelEncoderVals.length;
        }

        double theta = robotPos.getHeading();
        double deltaX = centralEncoderVal * Math.cos(theta + deltaTheta / 2);
        double deltaY = centralEncoderVal * Math.sin(theta + deltaTheta / 2);

        updateOdometryCounts(deltaX, deltaY);
    }

    /**
     * The method that updates the strafe values for the x and y coordinates of
     * the robot's position.
     *
     * @param deltaTheta                the change in the robot's orientation
     * @param perpendicularEncoderVal   the value of encoders for the perpendicular odometers
     */
    public void updateStrafe(double deltaTheta, double perpendicularEncoderVal) {
        double theta = robotPos.getHeading();
        double deltaX = perpendicularEncoderVal * Math.sin(theta + deltaTheta / 2);
        double deltaY = perpendicularEncoderVal * Math.cos(theta + deltaTheta / 2);

        updateOdometryCounts(-deltaX, deltaY);
    }

    /**
     * The update method that updates the robot's position over a relatively small amount of time.
     * The update frequency can range in a value of Hz. However many updates that are called per
     * second is the representation of the feedback frequency in Hertz (1/s). If we have
     * a frequency of 60 Hz, then the update method is called 60 times in 1 second.
     * If you have no heading, pass in a value of 0 for each update. If you have no horizontal odometers,
     * pass in a value of 0 for horizontalOdoInches.
     *
     * @param heading               the current heading of the robot, which might be de-synced
     *                              with the heading in the robot position.
     * @param horizontalOdoInches   the number of inches travelled by the horizontal odometers.
     * @param verticalOdoInches     the number of inches travelled by the vertical odometers.
     */
    public void update(double heading, double horizontalOdoInches, double... verticalOdoInches) {
        double phi = verticalOdoInches[verticalOdoInches.length - 1] - verticalOdoInches[0];
        phi /= trackWidth;
        double deltaTheta = (heading != 0) ? heading - robotPos.getHeading() : phi;

        updateCurve(deltaTheta, verticalOdoInches);
        updateStrafe(deltaTheta, horizontalOdoInches);

        rotatePose(deltaTheta);

        updatePose(new Pose2d(odoX, odoY, new Rotation2d(robotPos.getHeading())));
    }

}