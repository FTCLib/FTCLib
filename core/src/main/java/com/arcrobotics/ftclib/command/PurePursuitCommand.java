package com.arcrobotics.ftclib.command;

import com.arcrobotics.ftclib.drivebase.DifferentialDrive;
import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.ChassisSpeeds;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.DifferentialDriveKinematics;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.DifferentialDriveWheelSpeeds;
import com.arcrobotics.ftclib.trajectory.Trajectory;
import com.arcrobotics.ftclib.trajectory.purepursuit.PathFollower;

import java.util.function.Supplier;

/**
 * A command that makes use of the look-ahead algorithm for
 * following a path derived from a trajectory.
 *
 * @author Jackson
 */
public class PurePursuitCommand extends CommandBase {

    private DifferentialDrive drive;
    private DifferentialDriveKinematics kinematics;
    private PathFollower follower;
    private Translation2d poseTolerance;
    private Supplier<Pose2d> currentRobotPose;
    private double lookAhead, maxVelocity;

    public PurePursuitCommand(DifferentialDrive drive,
                              DifferentialDriveKinematics kinematics,
                              Trajectory trajectory,
                              double lookAheadDistance,
                              double maxVelocity,
                              Translation2d poseTolerance,
                              Supplier<Pose2d> currentRobotPose) {
        this.drive = drive;
        this.kinematics = kinematics;
        this.currentRobotPose = currentRobotPose;
        this.maxVelocity = maxVelocity;
        this.poseTolerance = poseTolerance;
        lookAhead = lookAheadDistance;
        follower = new PathFollower(trajectory, maxVelocity);
    }

    @Override
    public void execute() {
        Pose2d robotPose = currentRobotPose.get();
        ChassisSpeeds chassisSpeeds = follower.getChassisSpeeds(robotPose, lookAhead);
        DifferentialDriveWheelSpeeds wheelSpeeds = kinematics.toWheelSpeeds(chassisSpeeds);
        wheelSpeeds.normalize(maxVelocity);
        drive.tankDrive(wheelSpeeds.leftMetersPerSecond / maxVelocity, wheelSpeeds.rightMetersPerSecond / maxVelocity);
    }

    @Override
    public void end(boolean interrupted) {
        drive.stop();
    }

    @Override
    public boolean isFinished() {
        Pose2d robotPose = currentRobotPose.get();
        return !follower.isBusy(robotPose, poseTolerance);
    }

}
