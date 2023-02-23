package com.arcrobotics.ftclib.trajectory.purepursuit;

import android.net.TrafficStats;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.ChassisSpeeds;
import com.arcrobotics.ftclib.trajectory.Trajectory;

public class PathFollower {

    private Path path;
    private double maxVelo;
    private int currentPointIdx;

    public PathFollower(Trajectory trajectory, double maxVelocity) {
        path = new Path(trajectory);
        maxVelo = maxVelocity;
        currentPointIdx = 0;
    }

    public ChassisSpeeds getChassisSpeeds(Pose2d robotPose, double lookAhead) {
        currentPointIdx = path.getRelativeLookAheadPoint(robotPose, currentPointIdx, lookAhead);
        Translation2d deltaRobot = path.getPoint(currentPointIdx).relativeTo(robotPose).getTranslation();
        double curvature = 2 * deltaRobot.getY() / (lookAhead * lookAhead);
        return new ChassisSpeeds(maxVelo, 0, maxVelo * curvature);
    }

    public boolean isBusy(Pose2d robotPose, Translation2d poseTolerance) {
        if (currentPointIdx == path.getPath().size() - 1) {
            return false;
        }

        Pose2d lastPathPoint = path.getLastPoint();
        Translation2d pointDiff = robotPose.minus(lastPathPoint).getTranslation();
        return Math.abs(pointDiff.getX()) > poseTolerance.getX() || Math.abs(pointDiff.getY()) > poseTolerance.getY();
    }

    public Pose2d getCurrentPoint() {
        return path.getPoint(currentPointIdx);
    }

}
