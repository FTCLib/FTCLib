package com.example.ftclibexamples;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.arcrobotics.ftclib.kinematics.HolonomicOdometry;
import com.arcrobotics.ftclib.util.LUT;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "LUT Sample")
@Disabled
public class LookUpTableSample extends LinearOpMode {

    // our lookup table of distances from the goal and respective speeds of the shooter
    LUT<Double, Double> speeds = new LUT<Double, Double>() {{
        add(5.0, 1.0);
        add(4.0, 0.9);
        add(3.0, 0.75);
        add(2.0, 0.5);
        add(1.0, 0.2);
    }};

    private HolonomicOdometry odometry;
    private MotorEx leftEncoder, rightEncoder, perpEncoder;
    private Motor shooter;

    @Override
    public void runOpMode() throws InterruptedException {
        shooter = new Motor(hardwareMap, "shooter");

        leftEncoder = new MotorEx(hardwareMap, "left");
        rightEncoder = new MotorEx(hardwareMap, "right");
        perpEncoder = new MotorEx(hardwareMap, "perp");

        // REVcoders
        // the values we are setting here is the circumference of the
        // 2 inch odometer wheels in inches divided by 8192 (the CPR)
        leftEncoder.setDistancePerPulse(2 * Math.PI / (double) 8192);
        rightEncoder.setDistancePerPulse(2 * Math.PI / (double) 8192);
        perpEncoder.setDistancePerPulse(2 * Math.PI / (double) 8192);

        // The last two values are trackwidth and center_wheel_offset
        odometry = new HolonomicOdometry(
                leftEncoder::getDistance,
                rightEncoder::getDistance,
                perpEncoder::getDistance,
                14,
                2.1
        );

        odometry.updatePose(new Pose2d(3, 4, new Rotation2d(0)));

        waitForStart();

        // let's say our goal is at (5, 10) in our global field coordinates
        while (opModeIsActive() && !isStopRequested()) {
            if (gamepad1.a) {
                double distance = odometry.getPose().getTranslation().getDistance(new Translation2d(5, 10));
                shooter.set(speeds.getClosest(distance));
            }
            odometry.updatePose();
        }

    }

}
