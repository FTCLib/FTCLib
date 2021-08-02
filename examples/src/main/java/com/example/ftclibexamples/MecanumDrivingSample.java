package com.example.ftclibexamples;

import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.hardware.RevIMU;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp
public class MecanumDrivingSample extends LinearOpMode {

    static final boolean FIELD_CENTRIC = false;

    @Override
    public void runOpMode() throws InterruptedException {
        // constructor takes in frontLeft, frontRight, backLeft, backRight motors
        // IN THAT ORDER
        MecanumDrive drive = new MecanumDrive(
            new Motor(hardwareMap, "frontLeft", Motor.GoBILDA.RPM_435),
            new Motor(hardwareMap, "frontRight", Motor.GoBILDA.RPM_435),
            new Motor(hardwareMap, "backLeft", Motor.GoBILDA.RPM_435),
            new Motor(hardwareMap, "backRight", Motor.GoBILDA.RPM_435)
        );

        RevIMU imu = new RevIMU(hardwareMap);
        imu.init();

        GamepadEx driverOp = new GamepadEx(gamepad1);

        waitForStart();

        while (!isStopRequested()) {

            if (!FIELD_CENTRIC) {
                // optional fourth parameter for squared inputs
                drive.driveRobotCentric(
                        driverOp.getLeftX(),
                        driverOp.getLeftY(),
                        driverOp.getRightX(),
                        false
                );
            } else {
                // optional fifth parameter for squared inputs
                drive.driveFieldCentric(
                        driverOp.getLeftX(),
                        driverOp.getLeftY(),
                        driverOp.getRightX(),
                        imu.getRotation2d().getDegrees(),   // gyro value passed in here must be in degrees
                        false
                );
            }

        }
    }

}
