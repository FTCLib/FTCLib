package com.arcrobotics.ftclib.command;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.ChassisSpeeds;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.DifferentialDriveKinematics;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.DifferentialDriveWheelSpeeds;
import com.arcrobotics.ftclib.trajectory.Trajectory;
import com.arcrobotics.ftclib.trajectory.TrajectoryConfig;
import com.arcrobotics.ftclib.trajectory.purepursuit.PathFollower;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * A command that makes use of the look-ahead algorithm for
 * following a path derived from a trajectory.
 *
 * @author Jackson
 */
public class PurePursuitCommand extends CommandBase {

    private DifferentialDriveKinematics kinematics;
    private PathFollower follower;
    private Translation2d poseTolerance;
    private Supplier<Pose2d> currentRobotPose;
    private BiConsumer<Double, Double> output;
    private double lookAhead;

    public PurePursuitCommand(TrajectoryConfig config,
                              Trajectory trajectory,
                              DifferentialDriveKinematics kinematics,
                              double lookAheadDistance,
                              Translation2d poseTolerance,
                              Supplier<Pose2d> currentRobotPose,
                              BiConsumer<Double, Double> output) {
        this.kinematics = kinematics;
        this.currentRobotPose = currentRobotPose;
        this.poseTolerance = poseTolerance;
        this.output = output;
        lookAhead = lookAheadDistance;
        follower = new PathFollower(trajectory, config.getMaxVelocity());
    }

    @Override
    public void execute() {
        Pose2d robotPose = currentRobotPose.get();
        ChassisSpeeds chassisSpeeds = follower.getChassisSpeeds(robotPose, lookAhead);
        DifferentialDriveWheelSpeeds wheelSpeeds = kinematics.toWheelSpeeds(chassisSpeeds);
        wheelSpeeds.normalize(1);
        output.accept(wheelSpeeds.leftMetersPerSecond, wheelSpeeds.rightMetersPerSecond);
    }

    @Override
    public void end(boolean interrupted) {
        output.accept(0.0, 0.0);
    }

    @Override
    public boolean isFinished() {
        Pose2d robotPose = currentRobotPose.get();
        return !follower.isBusy(robotPose, poseTolerance);
    }

}
