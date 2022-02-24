package com.example.ftclibexamples;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.command.OdometrySubsystem;
import com.arcrobotics.ftclib.command.PurePursuitCommand;
import com.arcrobotics.ftclib.drivebase.DifferentialDrive;
import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.hardware.motors.MotorGroup;
import com.arcrobotics.ftclib.kinematics.DifferentialOdometry;
import com.arcrobotics.ftclib.kinematics.wpilibkinematics.DifferentialDriveKinematics;
import com.arcrobotics.ftclib.trajectory.Trajectory;
import com.arcrobotics.ftclib.trajectory.TrajectoryConfig;
import com.arcrobotics.ftclib.trajectory.TrajectoryGenerator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;

import java.util.Arrays;

/**
 * A sample for the pure pursuit command, which is a look-ahead based follower
 * for the FTCLib trajectories. Please refer to the DeadWheelsSample
 * before looking at this one, as it will more thoroughly explain
 * how the odometry is being used and how to create it.
 */
@Autonomous
@Disabled
public class PurePursuitSample extends CommandOpMode {

    static final double TRACKWIDTH = 0.4572;    // meters
    static final double WHEEL_DIAMETER = 0.03;  // meters
    static final double TICKS_PER_REV = 4096;
    static final double DISTANCE_PER_PULSE = WHEEL_DIAMETER * Math.PI / TICKS_PER_REV;

    static final double MAX_VELOCITY = 1.5;     // meters per second
    static final double MAX_ACCELERATION = 1.5; // meters per second per second

    static double LOOK_AHEAD_DISTANCE = 0.5;    // meters

    private DifferentialOdometry robotOdometry;
    private DifferentialDrive robotDrive;
    private Motor frontLeft, frontRight, backLeft, backRight;
    private Motor.Encoder leftEncoder, rightEncoder;

    private OdometrySubsystem odometrySubsystem;
    private PurePursuitCommand followerCommand;

    @Override
    public void initialize() {
        frontLeft = new Motor(hardwareMap, "front_left");
        frontRight = new Motor(hardwareMap, "front_right");
        backLeft = new Motor(hardwareMap, "back_left");
        backRight = new Motor(hardwareMap, "back_right");

        // The pure pursuit command uses a DifferentialDrive object
        // because of how the look-ahead algorithm works. It tries to make
        // the robot follow the curvature of the path through the use of the look ahead.
        robotDrive = new DifferentialDrive(
            new MotorGroup(frontLeft, backLeft),
            new MotorGroup(frontRight, backRight)
        );

        // obtain our encoder objects
        leftEncoder = frontLeft.encoder.setDistancePerPulse(DISTANCE_PER_PULSE);
        rightEncoder = frontRight.encoder.setDistancePerPulse(DISTANCE_PER_PULSE);

        // create our odometry object and subsystem
        robotOdometry = new DifferentialOdometry(leftEncoder::getDistance, rightEncoder::getDistance, TRACKWIDTH);
        odometrySubsystem = new OdometrySubsystem(robotOdometry);

        // Create config for trajectory
        DifferentialDriveKinematics kinematics = new DifferentialDriveKinematics(TRACKWIDTH);
        TrajectoryConfig config = new TrajectoryConfig(MAX_VELOCITY, MAX_ACCELERATION)
            .setKinematics(kinematics);

        // An example trajectory to follow. All units in meters.
        Trajectory exampleTrajectory =
            TrajectoryGenerator.generateTrajectory(
                // Start at the origin facing the +X direction
                new Pose2d(0, 0, new Rotation2d(0)),
                // Pass through these two interior waypoints, making an 's' curve path
                Arrays.asList(new Translation2d(1, 1), new Translation2d(2, -1)),
                // End 3 meters straight ahead of where we started, facing forward
                new Pose2d(3, 0, new Rotation2d(0)),
                // Pass config
                config);

        // create our pure pursuit command
        followerCommand = new PurePursuitCommand(
            config, exampleTrajectory,
            kinematics, LOOK_AHEAD_DISTANCE,
            new Translation2d(), odometrySubsystem::getPose,
            (leftSpeed, rightSpeed) -> robotDrive.tankDrive(leftSpeed, rightSpeed)
        );

        // schedule the command
        schedule(followerCommand);
    }

}
