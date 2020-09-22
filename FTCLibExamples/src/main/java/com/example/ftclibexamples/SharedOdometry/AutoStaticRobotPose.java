package com.example.ftclibexamples.SharedOdometry;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.hardware.RevIMU;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.arcrobotics.ftclib.kinematics.HolonomicOdometry;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

@Autonomous
@Disabled
public class AutoStaticRobotPose extends LinearOpMode {

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
                imu::getHeading,
                leftEncoder::getCurrentPosition,
                rightEncoder::getCurrentPosition,
                perpEncoder::getCurrentPosition,
                TRACKWIDTH,
                CENTER_WHEEL_OFFSET
        );

        waitForStart();

        while (!isStopRequested()) {
            // run autonomous

            // update positions
            odometry.updatePose();
            PositionTracker.robotPose = odometry.getPose();
        }
    }

}
