package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

import com.arcrobotics.ftclib.command.Subsystem;
import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.gamepad.ButtonReader;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.hardware.motors.MotorImpl;
import com.arcrobotics.ftclib.hardware.motors.MotorImplEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class DriveSubsystem implements Subsystem {

    GamepadEx driverGamepad;
    Telemetry telemetry;

    public MotorImplEx backLeftMotor, frontLeftMotor, backRightMotor, frontRightMotor;
    public MecanumDrive driveTrain;

    final double WHEEL_DIAMETER = 4; // Inches
    final double PULSES_PER_ROTATION = 280; // NEVEREST 40
    ButtonReader slowDownButton;


    public DriveSubsystem(GamepadEx driverGamepad, HardwareMap hw, Telemetry telemetry) {
        this.driverGamepad = driverGamepad;
        this.telemetry = telemetry;

        backLeftMotor = new MotorImplEx(new MotorImpl(hw, "backLeftMotor", PULSES_PER_ROTATION));
        frontLeftMotor = new MotorImplEx(new MotorImpl(hw, "frontLeftMotor", PULSES_PER_ROTATION));
        backRightMotor = new MotorImplEx(new MotorImpl(hw, "backRightMotor", PULSES_PER_ROTATION));
        frontRightMotor = new MotorImplEx(new MotorImpl(hw, "frontRightMotor", PULSES_PER_ROTATION));

        backLeftMotor.setInverted(true);
        frontLeftMotor.setInverted(true);

        backLeftMotor.setDistancePerPulse((WHEEL_DIAMETER * Math.PI) / (PULSES_PER_ROTATION));
        frontLeftMotor.setDistancePerPulse((WHEEL_DIAMETER * Math.PI) / (PULSES_PER_ROTATION));
        backRightMotor.setDistancePerPulse((WHEEL_DIAMETER * Math.PI) / (PULSES_PER_ROTATION));
        frontRightMotor.setDistancePerPulse((WHEEL_DIAMETER * Math.PI) / (PULSES_PER_ROTATION));


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

        driveTrain.driveRobotCentric(driverGamepad.getLeftY()  * maxSpeed,
                 driverGamepad.getLeftX() * maxSpeed, driverGamepad.getRightX() * maxSpeed);
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
