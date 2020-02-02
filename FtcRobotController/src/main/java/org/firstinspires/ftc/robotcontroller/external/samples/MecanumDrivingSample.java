package org.firstinspires.ftc.robotcontroller.external.samples;

import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.geometry.Vector2d;
import com.arcrobotics.ftclib.hardware.motors.MotorImplEx;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

public class MecanumDrivingSample extends LinearOpMode {

    private MecanumDrive driveTrain;

    // the motors used here are goBILDA Yellow Jackets, 435 rpm

    @Override
    public void runOpMode() throws InterruptedException {

        driveTrain = new MecanumDrive(
                new MotorImplEx(hardwareMap, "frontLeft", 383.6),
                new MotorImplEx(hardwareMap, "frontRight", 383.6),
                new MotorImplEx(hardwareMap, "backLeft", 383.6),
                new MotorImplEx(hardwareMap, "backRight", 383.6)
        );

        waitForStart();

        driveWithVector(new Vector2d(12,3));
        sleep(1000);
        driveWithVector(new Vector2d(0,0));

    }

    private void driveWithVector(Vector2d vector) {
        double m_x, m_y;

        if (vector.getX() > vector.getY() && vector.getX() > 1) {
            m_x = 1;
            m_y = vector.getY() / vector.getX();
        } else if (vector.getY() > 1) {
            m_x = vector.getX() / vector.getY();
            m_y = 1;
        } else {
            m_x = vector.getX();
            m_y = vector.getY();
        }

        driveTrain.driveRobotCentric(m_x, m_y,0);
    }

}
