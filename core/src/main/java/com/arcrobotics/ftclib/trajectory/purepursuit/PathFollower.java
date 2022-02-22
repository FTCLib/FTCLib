package com.arcrobotics.ftclib.trajectory.purepursuit;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.ChassisSpeeds;
import com.arcrobotics.ftclib.trajectory.Trajectory;

public class PathFollower {

    private Path path;
    private double tolerance, maxVelo;

    public PathFollower(Trajectory trajectory, double maxVelocity, double goalTolerance) {
        path = new Path(trajectory);
        tolerance = goalTolerance;
        maxVelo = maxVelocity;
    }

    public ChassisSpeeds getChassisSpeeds(Pose2d robotPose, double lookAhead) {
        Translation2d lookAheadPoint = path.getRelativeLookAheadPoint(robotPose, lookAhead);
        double curvature = lookAheadPoint.getX() / (lookAhead * lookAhead);
        return new ChassisSpeeds(maxVelo, 0, maxVelo * curvature);
    }

}
