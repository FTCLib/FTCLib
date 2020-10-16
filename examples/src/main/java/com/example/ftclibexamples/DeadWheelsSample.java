package com.example.ftclibexamples;

import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.hardware.motors.Motor.Encoder;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.arcrobotics.ftclib.kinematics.HolonomicOdometry;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

/**
 * This sample shows how to use dead wheels with external encoders
 * paired with motors that don't require encoders.
 * In this sample, we will use the drive motors' encoder
 * ports as they are not needed due to not using the drive encoders.
 * The external encoders we are using are REV through-bore.
 */
@Autonomous
@Disabled
public class DeadWheelsSample extends LinearOpMode {

    // The lateral distance between the left and right odometers
    // is called the trackwidth. This is very important for
    // determining angle for turning approximations
    public static final double TRACKWIDTH = 14.7;

    // Center wheel offset is the distance between the
    // center of rotation of the robot and the center odometer.
    // This is to correct for the error that might occur when turning.
    // A negative offset means the odometer is closer to the back,
    // while a positive offset means it is closer to the front.
    public static final double CENTER_WHEEL_OFFSET = -2.1;

    public static final double WHEEL_DIAMETER = 2.0;
    public static final double TICKS_PER_REV = 8192;

    private MotorEx frontLeft, frontRight, backLeft, backRight;
    private MecanumDrive driveTrain;
    private Motor intakeLeft, intakeRight, liftLeft, liftRight;
    private Encoder leftOdometer, rightOdometer, centerOdometer;
    private HolonomicOdometry odometry;

    @Override
    public void runOpMode() throws InterruptedException {
        frontLeft = new MotorEx(hardwareMap, "front_left");
        frontRight = new MotorEx(hardwareMap, "front_right");
        backLeft = new MotorEx(hardwareMap, "back_left");
        backRight = new MotorEx(hardwareMap, "back_right");

        driveTrain = new MecanumDrive(frontLeft, frontRight, backLeft, backRight);

        intakeLeft = new Motor(hardwareMap, "intake_left");
        intakeRight = new Motor(hardwareMap, "intake_right");
        liftLeft = new Motor(hardwareMap, "lift_left");
        liftRight = new Motor(hardwareMap, "lift_right");

        leftOdometer = frontLeft.encoder;
        rightOdometer = frontRight.encoder;
        centerOdometer = backLeft.encoder;

        odometry = new HolonomicOdometry(
                () -> leftOdometer.getPosition() / TICKS_PER_REV * WHEEL_DIAMETER * Math.PI,
                () -> rightOdometer.getPosition() / TICKS_PER_REV * WHEEL_DIAMETER * Math.PI,
                () -> centerOdometer.getPosition() / TICKS_PER_REV * WHEEL_DIAMETER * Math.PI,
                TRACKWIDTH, CENTER_WHEEL_OFFSET
        );

        waitForStart();

        while (opModeIsActive() && !isStopRequested()) {
            // control loop

            odometry.updatePose(); // update the position
        }
    }

}
