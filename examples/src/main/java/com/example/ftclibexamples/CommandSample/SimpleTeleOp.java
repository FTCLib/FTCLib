package com.example.ftclibexamples.CommandSample;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.command.InstantCommand;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Does the same thing as {@link SampleTeleOp} but is much simpler
 * in scope.
 * <p>
 * Note that the <b>proper</b> way to do this is with the SampleTeleOp version,
 * where most things are set up as commands/subsystems to avoid potential drawbacks
 * of the {@link InstantCommand}.
 */
@TeleOp
@Disabled
public class SimpleTeleOp extends CommandOpMode {

    private GamepadEx driverOp, toolOp;
    private GripperSubsystem gripper;
    private DriveSubsystem drive;
    private DefaultDrive driveCommand;

    @Override
    public void initialize() {
        driverOp = new GamepadEx(gamepad1);
        toolOp = new GamepadEx(gamepad2);

        gripper = new GripperSubsystem(hardwareMap, "gripper");
        drive = new DriveSubsystem(hardwareMap, "left", "right", 100.0);

        driveCommand = new DefaultDrive(drive, driverOp::getLeftY, driverOp::getRightX);

        // using InstantCommand here is not the greatest idea because the servos move in nonzero time
        // alternatives are adding WaitUntilCommands or making these commands.
        // As a result of this uncertainty, we add the gripper subsystem to ensure requirements are met.
        toolOp.getGamepadButton(GamepadKeys.Button.A)
                .whenPressed(new InstantCommand(gripper::grab, gripper));
        toolOp.getGamepadButton(GamepadKeys.Button.B)
                .whenPressed(new InstantCommand(gripper::release, gripper));

        register(drive);
        drive.setDefaultCommand(driveCommand);
    }

}