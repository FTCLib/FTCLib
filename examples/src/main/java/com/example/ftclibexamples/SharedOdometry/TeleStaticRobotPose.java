package com.example.ftclibexamples.SharedOdometry;

import com.arcrobotics.ftclib.hardware.RevIMU;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.arcrobotics.ftclib.kinematics.HolonomicOdometry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

public class TeleStaticRobotPose extends LinearOpMode {

    private MotorEx leftEncoder, rightEncoder, perpEncoder;
    private HolonomicOdometry odometry;
    private RevIMU imu;

    public static final double TRACKWIDTH = 14.31;
    public static final double CENTER_WHEEL_OFFSET = 0.477;

    @Override
    public void runOpMode() throws InterruptedException {
        leftEncoder = new MotorEx(hardwareMap, "left odometer");
        rightEncoder = new MotorEx(hardwareMap, "right odometer");
        perpEncoder = new MotorEx(hardwareMap, "center odometer");
        imu = new RevIMU(hardwareMap);

        imu.init();

        odometry = new HolonomicOdometry(
                imu.getRotation2d(),
                leftEncoder::getCurrentPosition,
                rightEncoder::getCurrentPosition,
                perpEncoder::getCurrentPosition,
                TRACKWIDTH,
                CENTER_WHEEL_OFFSET
        );

        // read the current position from the position tracker
        odometry.updatePose(PositionTracker.robotPose);

        telemetry.addData("Robot Position at Init: ", PositionTracker.robotPose);
        telemetry.update();

        waitForStart();

        while (opModeIsActive() && !isStopRequested()) {
            // teleop things

            // update position
            odometry.updatePose();
            PositionTracker.robotPose = odometry.getPose();
        }
    }

}
