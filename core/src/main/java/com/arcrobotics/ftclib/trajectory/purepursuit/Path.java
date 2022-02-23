package com.arcrobotics.ftclib.trajectory.purepursuit;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

public class Path {

    private List<Translation2d> pathPoints;
    private int lastLookAheadPoint;

    public Path(Trajectory trajectory) {
        pathPoints = new ArrayList<>();
        for (double t = 0; t < trajectory.getTotalTimeSeconds(); t += 0.01) {
            Trajectory.State currSample = trajectory.sample(t);
            Translation2d point = new Translation2d(currSample.poseMeters.getX(), currSample.poseMeters.getY());
            pathPoints.add(point);
        }
    }

    public Translation2d getRelativeLookAheadPoint(Pose2d robotPose, double lookAhead) {
        Translation2d robotPoint = robotPose.getTranslation();
        int closestPointIdx = lastLookAheadPoint;
        for (int pointIdx = Math.min(closestPointIdx - 1, 0); pointIdx < pathPoints.size(); pointIdx++) {
            Translation2d relativePoint = pathPoints.get(pointIdx).minus(robotPoint);
            Translation2d closestRelativePoint = pathPoints.get(closestPointIdx).minus(robotPoint);
            double d1 = Math.sqrt(lookAhead * lookAhead - relativePoint.getX() * relativePoint.getX());
            double d2 = Math.sqrt(lookAhead * lookAhead - closestRelativePoint.getX() * closestRelativePoint.getX());
            double diff1 = Math.abs(relativePoint.getY() - d1);
            double diff2 = Math.abs(closestRelativePoint.getY() - d2);
            if (diff1 < diff2) {
                closestPointIdx = pointIdx;
            }
        }
        lastLookAheadPoint = closestPointIdx;
        return pathPoints.get(closestPointIdx).minus(robotPoint);
    }

    public Translation2d getLastPoint() {
        return pathPoints.get(pathPoints.size() - 1);
    }

}
