package com.arcrobotics.ftclib.trajectory;

import com.arcrobotics.ftclib.R;
import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Transform2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.ChassisSpeeds;
import com.arcrobotics.ftclib.trajectory.purepursuit.Path;
import com.arcrobotics.ftclib.trajectory.purepursuit.PathFollower;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class PurePursuitTest {

    private void print(String msg) {
        System.out.println(msg);
    }

    private Trajectory testTrajectory() {
        TrajectoryConfig config = new TrajectoryConfig(1.5, 1.5);
        return TrajectoryGenerator.generateTrajectory(
                new Pose2d(0, 0, new Rotation2d(0)),
                Arrays.asList(new Translation2d(1, 1), new Translation2d(-2, 1)),
                new Pose2d(3, 0, new Rotation2d(0)),
                config
        );
    }

    @Test
    public void testPathGeneration() {
        Path p = new Path(testTrajectory());
        print(p.getPath().toString());
    }

    @Test
    public void testRelativeLookAheadPoint() {
        Pose2d currentPose = new Pose2d(new Translation2d(0, 0), new Rotation2d(0));
        Path p = new Path(testTrajectory());
        int lookAheadIdx = 0;
        double lookAhead = 0.1;
        while (lookAheadIdx !=  p.getPath().size() - 1) {
            lookAheadIdx = p.getRelativeLookAheadPoint(currentPose, lookAheadIdx, lookAhead);
            print(p.getPoint(lookAheadIdx).toString());
            currentPose = p.getPoint(lookAheadIdx);
        }
    }

    @Test
    public void testPathFollowerChassisSpeeds() {
        Pose2d currentPose = new Pose2d(new Translation2d(0, 0), new Rotation2d(0));
        PathFollower follower = new PathFollower(testTrajectory(), 0.5);
        double lookAhead = 0.05;
        while (follower.isBusy(currentPose, new Translation2d(0.1, 0.1))) {
            ChassisSpeeds speeds = follower.getChassisSpeeds(currentPose, lookAhead);
            print(speeds.toString());
            Transform2d delta = follower.getCurrentPoint().minus(currentPose).times(0.5);
            currentPose = currentPose.plus(delta);
        }
    }

}
