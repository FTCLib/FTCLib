package com.example.ftclibexamples.CommandSample;

import com.arcrobotics.ftclib.command.Robot;
import com.arcrobotics.ftclib.command.button.GamepadButton;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

public class SampleRobot extends Robot {

    static final double WHEEL_DIAMETER = 100.0; // centimeters
    static final double DRIVE_CPR = 383.6;

    private LinearOpMode m_opMode;

    private MotorEx m_left, m_right;
    private DriveSubsystem m_drive;
    private GamepadEx m_driverOp;
    private DefaultDrive m_driveCommand;
    private GripperSubsystem m_gripper;
    private GrabStone m_grabCommand;
    private ReleaseStone m_releaseCommand;
    private GamepadButton m_grabButton;
    private GamepadButton m_releaseButton;

    public SampleRobot(LinearOpMode opMode, boolean teleop) {
        m_opMode = opMode;

        initHardware();

        if (teleop) initTeleOp();
    }

    public void initHardware() {
        m_left = new MotorEx(m_opMode.hardwareMap, "drive_left");
        m_right = new MotorEx(m_opMode.hardwareMap, "drive_right");
        m_drive = new DriveSubsystem(m_left, m_right, WHEEL_DIAMETER);

        m_driverOp = new GamepadEx(m_opMode.gamepad1);
        m_driveCommand = new DefaultDrive(m_drive, ()->m_driverOp.getLeftY(), ()->m_driverOp.getLeftX());

        m_grabButton = new GamepadButton(m_driverOp, GamepadKeys.Button.A);
        m_releaseButton = new GamepadButton(m_driverOp, GamepadKeys.Button.B);

        m_gripper = new GripperSubsystem(m_opMode.hardwareMap, "gripper");
        m_grabCommand = new GrabStone(m_gripper);
        m_releaseCommand = new ReleaseStone(m_gripper);
    }

    public void initTeleOp() {
        m_drive.setDefaultCommand(m_driveCommand);
        m_grabButton.whenPressed(m_grabCommand);
        m_releaseButton.whenPressed(m_releaseCommand);
        register(m_drive, m_gripper);
    }

}
