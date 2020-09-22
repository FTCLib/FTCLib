package org.firstinspires.ftc.robotcontroller.external.samples.MecanumOdometrySample.FTCLibCommandSample;

import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "Command-Based Teleop Sample", group = "Command")
public class Teleop extends OpMode {

    DriveSubsystem driveSubsystem;
    GamepadEx driverGamepad;

    @Override
    public void init() {
        driverGamepad = new GamepadEx(gamepad1);
        driveSubsystem = new DriveSubsystem(driverGamepad, hardwareMap, telemetry);

        driveSubsystem.initialize();
    }

    @Override
    public void loop() {
        driveSubsystem.loop();
        telemetry.update();
    }
}
