package org.firstinspires.ftc.robotcontroller.external.samples;

import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.arcrobotics.ftclib.geometry.Translation2d;
import com.arcrobotics.ftclib.hardware.JSTEncoder;
import com.arcrobotics.ftclib.hardware.RevIMU;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.kinematics.HolonomicOdometry;
import com.arcrobotics.ftclib.util.LUT;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "LUT Sample")
@Disabled
public class LookUpTableSample extends LinearOpMode
{

    // our lookup table of distances from the goal and respective speeds of the shooter
    LUT<Double, Double> speeds = new LUT<Double, Double>()
    {{
        add(5.0, 1.0);
        add(4.0, 0.9);
        add(3.0, 0.75);
        add(2.0, 0.5);
        add(1.0, 0.2);
    }};

    private HolonomicOdometry odometry;
    private JSTEncoder leftEncoder, rightEncoder, perpEncoder;
    private Motor shooter = new Motor(hardwareMap, "shooter");
    private RevIMU gyro = new RevIMU(hardwareMap);

    @Override
    public void runOpMode() throws InterruptedException
    {

        leftEncoder = new JSTEncoder(hardwareMap, "left");
        rightEncoder = new JSTEncoder(hardwareMap, "right");
        perpEncoder = new JSTEncoder(hardwareMap, "perp");

        // REVcoders
        leftEncoder.setDistancePerPulse(2 / (double) 8092);
        rightEncoder.setDistancePerPulse(2 / (double) 8092);
        perpEncoder.setDistancePerPulse(2 / (double) 8092);

        odometry = new HolonomicOdometry(
                gyro::getHeading,
                leftEncoder::getDistance,
                rightEncoder::getDistance,
                perpEncoder::getDistance,
                14,
                2.1
        );

        odometry.updatePose(new Pose2d(3, 4, new Rotation2d(0)));

        waitForStart();

        // let's say our goal is at (5, 10) in our global field coordinates
        while (opModeIsActive() && !isStopRequested())
        {
            if (gamepad1.a)
            {
                double distance = odometry.getPose().getTranslation().getDistance(new Translation2d(5, 10));
                shooter.set(speeds.getClosest(distance));
            }
            odometry.updatePose();
        }

    }

}
