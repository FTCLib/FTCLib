package com.arcrobotics.ftclib.kinematics;

import com.arcrobotics.ftclib.geometry.Pose2d;

public class MecanumOdometry {

    public Pose2d robotPos;

    public double odoX;
    public double odoY;

    private double trackWidth;

    public MecanumOdometry() {
        this(18);
    }

    public MecanumOdometry(double trackWidth) {
        this(new Pose2d(0, 0, 0), trackWidth);
    }

    public MecanumOdometry(Pose2d pose, double trackWidth) {
        robotPos = pose;
        this.trackWidth = trackWidth;
    }

    public void updatePose(Pose2d newPose) {
        robotPos = new Pose2d(newPose);
    }

    public void rotatePose(double deltaTheta) {
        robotPos = robotPos.rotate(deltaTheta);
    }

    public void updateOdometryCounts(double xVal, double yVal) {
        odoX += xVal;
        odoY += yVal;
    }

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

    private void updateStrafe(double deltaTheta, double perpendicularEncoderVal) {
        double theta = robotPos.getHeading();
        double deltaX = perpendicularEncoderVal * Math.sin(theta + deltaTheta / 2);
        double deltaY = perpendicularEncoderVal * Math.cos(theta + deltaTheta / 2);

        updateOdometryCounts(-deltaX, deltaY);
    }

    public void update(double horizontalOdoInches, double... verticalOdoInches) {
        double deltaTheta =
                verticalOdoInches[verticalOdoInches.length - 1] - verticalOdoInches[0];
        deltaTheta /= trackWidth;
        updateCurve(deltaTheta, verticalOdoInches);
        updateStrafe(deltaTheta, horizontalOdoInches);

        rotatePose(deltaTheta);
    }

    public void update(double horizontalOdoInches, double verticalOdoInches, double heading) {
        double deltaTheta = heading - robotPos.getHeading();
        updateCurve(deltaTheta, verticalOdoInches);
        updateStrafe(deltaTheta, horizontalOdoInches);

        rotatePose(deltaTheta);
    }

}