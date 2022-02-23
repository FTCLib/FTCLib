package com.arcrobotics.ftclib.trajectory.purepursuit;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.ChassisSpeeds;
import com.arcrobotics.ftclib.trajectory.Trajectory;

public class PathFollower {

    private Path path;
    private double maxVelo;

    public PathFollower(Trajectory trajectory, double maxVelocity) {
        path = new Path(trajectory);
        maxVelo = maxVelocity;
    }

    public ChassisSpeeds getChassisSpeeds(Pose2d robotPose, double lookAhead) {
        Translation2d lookAheadPoint = path.getRelativeLookAheadPoint(robotPose, lookAhead);
        double curvature = lookAheadPoint.getX() / (lookAhead * lookAhead);
        return new ChassisSpeeds(maxVelo, 0, maxVelo * curvature);
    }

    public boolean isBusy(Pose2d robotPose, Translation2d poseTolerance) {
        Translation2d robotPoint = robotPose.getTranslation();
        Translation2d lastPathPoint = path.getLastPoint();
        Translation2d pointDiff = robotPoint.minus(lastPathPoint);
        return Math.abs(pointDiff.getX()) > poseTolerance.getX() || Math.abs(pointDiff.getY()) > poseTolerance.getY();
    }

}
