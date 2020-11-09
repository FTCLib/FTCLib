package com.example.ftclibexamples.Odometry;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.command.MecanumDriveCommand;
import com.arcrobotics.ftclib.command.MecanumSubsystem;
import com.arcrobotics.ftclib.command.OdometrySubsystem;
import com.arcrobotics.ftclib.command.PIDFCommand;
import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.hardware.motors.Motor.Encoder;
import com.arcrobotics.ftclib.kinematics.HolonomicOdometry;

import java.util.function.DoubleSupplier;

public class PIDGoToPointSample extends CommandOpMode {

    public static final double DISTANCE_PER_PULSE = DeadWheelsSample.DISTANCE_PER_PULSE;
    public static final double TRACKWIDTH = DeadWheelsSample.TRACKWIDTH;
    public static final double CENTER_WHEEL_OFFSET = DeadWheelsSample.CENTER_WHEEL_OFFSET;

    // we do PID on the x, y, and heading of the robot
    private PIDFCommand pidX, pidY, pidHeading;

    private Motor frontLeft, frontRight, backLeft, backRight;
    private Encoder leftOdometer, rightOdometer, centerOdometer;
    private MecanumDrive drive;
    private MecanumDriveCommand driveCommand;
    private MecanumSubsystem robotDrive;
    private HolonomicOdometry odometry;
    private OdometrySubsystem robotOdometry;

    private DoubleSupplier xSpeed, ySpeed, turnSpeed;

    @Override
    public void initialize() {
        frontLeft = new Motor(hardwareMap, "front_left", Motor.GoBILDA.RPM_312);
        frontRight = new Motor(hardwareMap, "front_right", Motor.GoBILDA.RPM_312);
        backLeft = new Motor(hardwareMap, "back_left", Motor.GoBILDA.RPM_312);
        backRight = new Motor(hardwareMap, "back_right", Motor.GoBILDA.RPM_312);

        drive = new MecanumDrive(frontLeft, frontRight, backLeft, backRight);
        robotDrive = new MecanumSubsystem(drive);

        /**
         * @see DeadWheelsSample
         */
        leftOdometer = frontLeft.encoder.setDistancePerPulse(DISTANCE_PER_PULSE);
        rightOdometer = frontRight.encoder.setDistancePerPulse(DISTANCE_PER_PULSE);
        centerOdometer = backLeft.encoder.setDistancePerPulse(DISTANCE_PER_PULSE);
        rightOdometer.setDirection(Motor.Direction.REVERSE);

        odometry = new HolonomicOdometry(
                leftOdometer::getDistance,
                rightOdometer::getDistance,
                centerOdometer::getDistance,
                TRACKWIDTH, CENTER_WHEEL_OFFSET
        );
        robotOdometry = new OdometrySubsystem(odometry);

        /* Here is the crux of the PID Go To Point */
        /* This example moves the robot to (5, 5) with a heading of pi/4 */
        pidX = new PIDFCommand(robotOdometry.getPose()::getX, s -> xSpeed = () -> s)
                .setSetPoint(5)
                .setP(0.1)
                .setD(0.07);

        pidY = new PIDFCommand(robotOdometry.getPose()::getY, s -> ySpeed = () -> s)
                .setSetPoint(5)
                .setP(0.3)
                .setI(0.13)
                .setD(0.22);

        pidHeading = new PIDFCommand(robotOdometry.getPose()::getHeading, s -> turnSpeed = () -> s)
                .setSetPoint(Math.PI / 4)
                .setP(0.05);

        driveCommand = new MecanumDriveCommand(robotDrive, ySpeed, xSpeed, turnSpeed);

        schedule(pidX, pidY, pidHeading, driveCommand);
        register(robotOdometry, robotDrive);
    }

}
