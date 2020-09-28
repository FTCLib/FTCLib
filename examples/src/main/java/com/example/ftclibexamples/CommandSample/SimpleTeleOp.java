package com.example.ftclibexamples.CommandSample;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.command.InstantCommand;
import com.arcrobotics.ftclib.command.button.GamepadButton;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.hardware.SimpleServo;

/**
 * Does the same thing as {@link SampleTeleOp} but is much simpler
 * in scope.
 */
public class SimpleTeleOp extends CommandOpMode {

    private GamepadEx toolOp = new GamepadEx(gamepad2);
    private GamepadEx driverOp = new GamepadEx(gamepad1);
    private GamepadButton grabButton = new GamepadButton(toolOp, GamepadKeys.Button.A);
    private GamepadButton releaseButton = new GamepadButton(toolOp, GamepadKeys.Button.B);
    private SimpleServo servo = new SimpleServo(hardwareMap, "gripper");
    private DriveSubsystem drive =
            new DriveSubsystem(hardwareMap, "left", "right", 100.0);
    private DefaultDrive driveCommand = new DefaultDrive(drive, driverOp::getLeftY, driverOp::getRightX);

    @Override
    public void runOpMode() throws InterruptedException {
        grabButton.whenPressed(new InstantCommand(() -> servo.setPosition(0.76)));
        releaseButton.whenPressed(new InstantCommand(() -> servo.setPosition(0)));

        register(drive);

        // run the scheduler
        while (!isStopRequested()) {
            run();
        }

        reset();
    }

}
