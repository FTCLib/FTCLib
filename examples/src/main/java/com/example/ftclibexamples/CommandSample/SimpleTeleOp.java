package com.example.ftclibexamples.CommandSample;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.command.InstantCommand;
import com.arcrobotics.ftclib.command.button.GamepadButton;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * Does the same thing as {@link SampleTeleOp} but is much simpler
 * in scope.
 *
 * Note that the <b>proper</b> way to do this is with the SampleTeleOp version,
 * where most things are set up as commands/subsystems to avoid potential drawbacks
 * of the {@link InstantCommand}.
 */
@TeleOp
@Disabled
public class SimpleTeleOp extends CommandOpMode {

    private GamepadButton grabButton = new GamepadButton(toolOp, GamepadKeys.Button.A);
    private GamepadButton releaseButton = new GamepadButton(toolOp, GamepadKeys.Button.B);
    private GripperSubsystem gripper = new GripperSubsystem(hardwareMap, "gripper");
    private DriveSubsystem drive =
            new DriveSubsystem(hardwareMap, "left", "right", 100.0);
    private DefaultDrive driveCommand = new DefaultDrive(drive, driverOp::getLeftY, driverOp::getRightX);

    @Override
    public void initialize() {
        register(drive, gripper);
        // using InstantCommand here is not the greatest idea because the servos move in nonzero time
        // alternatives are adding WaitUntilCommands or making these commands.
        // As a result of this uncertainty, we add the gripper subsystem to ensure requirements are met.
        grabButton.whenPressed(new InstantCommand(gripper::grab, gripper));
        releaseButton.whenPressed(new InstantCommand(gripper::release, gripper));

        drive.setDefaultCommand(driveCommand);
    }

}