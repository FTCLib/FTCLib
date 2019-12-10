package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.gamepad.GamepadEx;

@com.qualcomm.robotcore.eventloop.opmode.Autonomous(name="Command-based Autonomous Sample")  // @Autonomous(...) is the other common choice
public class Auto extends CommandOpMode {

    DriveSubsystem driveSubsystem;
    GamepadEx driverGamepad;

    @Override
    public void initialize() {
        driverGamepad = new GamepadEx(gamepad1);
        driveSubsystem = new DriveSubsystem(driverGamepad, hardwareMap, telemetry);

        driveSubsystem.initialize();
    }

    @Override
    public void run() {
        // Drive Forward at half speed for four seconds or until it travels 10 units.
        addSequential(new DriveForwardCommand(driveSubsystem, 10, 0.5, 4));
    }
}
