package com.example.ftclibexamples;

import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.hardware.RevIMU;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.hardware.motors.Motor.GoBILDA;
import com.arcrobotics.ftclib.util.FTCLibOpMode;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * A sample for the better OpMode version in FTCLib,
 * which contains two {@link com.arcrobotics.ftclib.gamepad.GamepadEx}
 * objects and reads their buttons every loop.
 */
@TeleOp
@Disabled
public class OpModeSample extends FTCLibOpMode {

    private RevIMU imu;
    private Motor frontLeft, frontRight, backLeft, backRight;
    private MecanumDrive drive;

    @Override
    public void initialize() {
        imu = new RevIMU(hardwareMap);
        imu.init();

        frontLeft = new Motor(hardwareMap, "front_left", GoBILDA.RPM_435);
        frontRight = new Motor(hardwareMap, "front_right", GoBILDA.RPM_435);
        backLeft = new Motor(hardwareMap, "back_left", GoBILDA.RPM_435);
        backRight = new Motor(hardwareMap, "back_right", GoBILDA.RPM_435);

        drive = new MecanumDrive(frontLeft, frontRight, backLeft, backRight);
    }

    @Override
    public void runLoop() {
        drive.driveFieldCentric(
                gamepadEx1.getLeftX(), gamepadEx1.getLeftY(),
                gamepadEx1.getRightX(), imu.getHeading()
        );
    }

}
