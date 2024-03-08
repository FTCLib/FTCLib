package com.arcrobotics.ftclib.trajectory.purepursuit;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.trajectory.Trajectory;

import java.util.ArrayList;
import java.util.List;

public class Path {

    private List<Pose2d> pathPoints;

    public Path(Trajectory trajectory) {
        pathPoints = new ArrayList<>();
        for (double t = 0; t < trajectory.getTotalTimeSeconds(); t += 0.01) {
            Trajectory.State currSample = trajectory.sample(t);
            pathPoints.add(currSample.poseMeters);
        }
    }

    public int getRelativeLookAheadPoint(Pose2d robotPose, int lastLookAheadPointIdx, double lookAhead) {
        Translation2d robotPoint = robotPose.getTranslation();
        int i;
        double distance = 0;
        for (i = lastLookAheadPointIdx + 1; i < pathPoints.size(); i++) {
            distance += robotPoint.getDistance(getPoint(i).getTranslation());
            if (distance >= lookAhead) {
                break;
            }
        }
        return Math.min(i, pathPoints.size() - 1);
    }

    public Pose2d getLastPoint() {
        return pathPoints.get(pathPoints.size() - 1);
    }

    public List<Pose2d> getPath() {
        return pathPoints;
    }

    public Pose2d getPoint(int pointIdx) {
        return pathPoints.get(pointIdx);
    }

}
