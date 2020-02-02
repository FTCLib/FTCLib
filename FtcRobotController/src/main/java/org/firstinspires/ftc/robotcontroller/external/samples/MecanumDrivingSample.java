package org.firstinspires.ftc.robotcontroller.external.samples;

import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.geometry.Vector2d;
import com.arcrobotics.ftclib.hardware.motors.MotorImplEx;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

public class MecanumDrivingSample extends LinearOpMode {

    private MecanumDrive driveTrain;
    private MotorImplEx fL, fR, bL, bR;

    // the motors used here are goBILDA Yellow Jackets, 435 rpm

    @Override
    public void runOpMode() throws InterruptedException {
        fL = new MotorImplEx(hardwareMap, "frontLeft", 383.6);
        fR = new MotorImplEx(hardwareMap, "frontRight", 383.6);
        bL = new MotorImplEx(hardwareMap, "backLeft", 383.6);
        bR = new MotorImplEx(hardwareMap, "backRight", 383.6);

        driveTrain = new MecanumDrive(fL, fR, bL, bR);

        waitForStart();

        driveWithVector(new Vector2d(12,3));
    }

    public void driveWithVector(Vector2d vector) {
        driveTrain.driveRobotCentric(vector.getX(), vector.getY(),0);
    }


}
