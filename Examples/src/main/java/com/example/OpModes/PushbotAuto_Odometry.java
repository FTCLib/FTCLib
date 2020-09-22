package com.example.OpModes;

import com.arcrobotics.ftclib.drivebase.DifferentialDrive;
import com.arcrobotics.ftclib.hardware.RevIMU;
import com.arcrobotics.ftclib.hardware.motors.EncoderEx;
import com.arcrobotics.ftclib.hardware.motors.SimpleMotorEx;
import com.arcrobotics.ftclib.util.Safety;
import com.arcrobotics.ftclib.util.Timing;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.HardwarePushbot;

import java.util.concurrent.TimeUnit;

@Autonomous(name="Sample Autonomous with Odometry for Pushbot")
@Disabled
public class PushbotAuto_Odometry extends LinearOpMode {

    public static final double INCHES_PER_REV = 3 * 2 * Math.PI;

    private DifferentialDrive dt;
    private SimpleMotorEx left, right;
    private EncoderEx leftEncoder, rightEncoder;
    private RevIMU imu;

    private HardwarePushbot robot;

    @Override
    public void runOpMode() throws InterruptedException {
        left = new SimpleMotorEx("left", hardwareMap, 383.6);
        right = new SimpleMotorEx("right", hardwareMap, 383.6);

        leftEncoder = new EncoderEx(left);
        rightEncoder = new EncoderEx(right);

        dt = new DifferentialDrive(left, right);
        imu = new RevIMU(hardwareMap);

        robot = new HardwarePushbot(dt);

        waitForStart();

        telemetry.addData("Robot Position", robot.getRobotPosition().toString());
        telemetry.update();

        Timing.Timer randomTimer = new Timing.Timer(2, TimeUnit.SECONDS);
        randomTimer.start();

        while (!randomTimer.done()) {
            driveRandomly(0.1, -0.3);
        }
        stopRobot();

        randomTimer.pause();

        telemetry.addData("Robot Position", robot.getRobotPosition().toString());
        telemetry.update();
    }

    private void driveRandomly(double ySpeed, double turnSpeed) {
        robot.driveRobot(ySpeed, turnSpeed);
        try {
            robot.updateRobotPosition(imu.getHeading(),
                    leftEncoder.getNumRevolutions() * INCHES_PER_REV,
                    rightEncoder.getNumRevolutions() * INCHES_PER_REV);
        } catch (Exception e) {
            telemetry.addData("Error Thrown", e.getMessage());
            telemetry.update();
        }
    }

    private void stopRobot() {
        try {
            robot.stop(Safety.BREAK);
        } catch (Exception e) {
            telemetry.addData("Error Thrown", e.getMessage());
            telemetry.update();
        }
    }

}
