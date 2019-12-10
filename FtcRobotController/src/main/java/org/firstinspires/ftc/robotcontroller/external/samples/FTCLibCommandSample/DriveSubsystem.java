package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

import com.arcrobotics.ftclib.command.Subsystem;
import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.gamepad.ButtonReader;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.hardware.motors.GenericMotorEx;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.Hardware;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class DriveSubsystem implements Subsystem {

    GamepadEx driverGamepad;
    Telemetry telemetry;

    public GenericMotorEx backLeftMotor, frontLeftMotor, backRightMotor, frontRightMotor;
    public MecanumDrive driveTrain;

    ButtonReader slowDownButton;


    public DriveSubsystem(GamepadEx driverGamepad, HardwareMap hw, Telemetry telemetry) {
        this.driverGamepad = driverGamepad;
        this.telemetry = telemetry;

        backLeftMotor = new GenericMotorEx(hw, "backLeftMotor");
        frontLeftMotor = new GenericMotorEx(hw, "frontLeftMotor");
        backRightMotor = new GenericMotorEx(hw, "backRightMotor");
        frontRightMotor = new GenericMotorEx(hw, "frontRightMotor");

        backLeftMotor.setInverted(true);
        frontLeftMotor.setInverted(true);

        backLeftMotor.setDistancePerPulse(0.001);
        frontLeftMotor.setDistancePerPulse(0.001);
        backRightMotor.setDistancePerPulse(0.001);
        frontRightMotor.setDistancePerPulse(0.001);


        driveTrain = new MecanumDrive(frontLeftMotor, backLeftMotor, frontRightMotor, backRightMotor);

        slowDownButton = new ButtonReader(driverGamepad, GamepadKeys.Button.X);
    }

    @Override
    public void initialize() {
        reset();
    }

    @Override
    public void reset() {
        backLeftMotor.resetEncoder();
        backRightMotor.resetEncoder();
        frontLeftMotor.resetEncoder();
        frontRightMotor.resetEncoder();
    }

    @Override
    public void loop() {
        // Update the joystick values
        slowDownButton.readValue();

        double maxSpeed;

        if(slowDownButton.isDown())
            maxSpeed = 0.5;
        else
            maxSpeed = 1;

        driveTrain.driveRobotCentric(driverGamepad.getLeftX() * maxSpeed,
                driverGamepad.getLeftY()  * maxSpeed, driverGamepad.getRightX() * maxSpeed);
    }

    @Override
    public void stop() {
        driveTrain.driveRobotCentric(0, 0, 0);
    }

    @Override
    public void disable() {
        stop();
    }
}
