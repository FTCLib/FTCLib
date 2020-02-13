package org.firstinspires.ftc.teamcode.TestProject;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.hardware.motors.MotorImplEx;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "Test Project", group = "Command")
public class Teleop extends OpMode {

    public static final double kP = 0.003;
    public static final double kI = 0.12;
    public static final double kD = 0.053;
    public static final double kF = 0.07;
    public static final double kThreshold = 8;

    private DriveSubsystem driveSubsystem;
    private GamepadEx driverGamepad;


    @Override
    public void init() {
        driverGamepad = new GamepadEx(gamepad1);
        driveSubsystem = new DriveSubsystem(driverGamepad, hardwareMap, telemetry);



        driveSubsystem.initialize();


    }

    @Override
    public void loop() {
        telemetry.addData("Heading ", driveSubsystem.getHeading());
        telemetry.update();
    }
}
