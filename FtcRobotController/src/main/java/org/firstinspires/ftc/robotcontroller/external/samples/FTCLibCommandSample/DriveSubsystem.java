package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

import com.arcrobotics.ftclib.command.Subsystem;
import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.gamepad.ButtonReader;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.hardware.RevIMU;
import com.arcrobotics.ftclib.hardware.motors.EncoderEx;
import com.arcrobotics.ftclib.hardware.motors.SimpleMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class DriveSubsystem implements Subsystem {

    GamepadEx driverGamepad;
    Telemetry telemetry;

    //Gyro
    RevIMU gyro;

    private SimpleMotorEx backLeftMotor, frontLeftMotor, backRightMotor, frontRightMotor;
    public MecanumDrive driveTrain;

    EncoderEx backLeftEncoder, frontLeftEncoder, backRightEncoder, frontRightEncoder;

    final double WHEEL_DIAMETER = 4; // Inches
    final int PULSES_PER_ROTATION = 280; // NEVEREST 40

    ButtonReader slowDownButton;

    public DriveSubsystem(GamepadEx driverGamepad, HardwareMap hw, Telemetry telemetry) {
        this.driverGamepad = driverGamepad;
        this.telemetry = telemetry;

        backLeftMotor = new SimpleMotorEx("backLeftMotor", hw, 383.6);
        frontLeftMotor = new SimpleMotorEx("frontLeftMotor", hw, 383.6);
        backRightMotor = new SimpleMotorEx("backRightMotor", hw, 383.6);
        frontRightMotor = new SimpleMotorEx("frontRightMotor", hw, 383.6);

        frontLeftEncoder = new EncoderEx(frontLeftMotor);
        backLeftEncoder = new EncoderEx(backLeftMotor);
        frontRightEncoder = new EncoderEx(frontRightMotor);
        backRightEncoder = new EncoderEx(backRightMotor);

        gyro = new RevIMU(hw);

        backLeftMotor.setInverted(true);
        frontLeftMotor.setInverted(true);

        driveTrain = new MecanumDrive(false, frontLeftMotor, frontRightMotor, backLeftMotor, backRightMotor);

        slowDownButton = new ButtonReader(driverGamepad, GamepadKeys.Button.X);
    }

    public double getHeading() {
        return gyro.getHeading();
    }

    public boolean atTargetPos() {
        return  frontLeftEncoder.reachedTarget() &&
                backLeftEncoder.reachedTarget() &&
                frontRightEncoder.reachedTarget() &&
                backRightEncoder.reachedTarget();
    }

    public void driveToPosition(int target) {
        target *= PULSES_PER_ROTATION / WHEEL_DIAMETER; // convert from inches -> ticks

        frontLeftEncoder.runToPosition(target);
        backLeftEncoder.runToPosition(target);
        frontRightEncoder.runToPosition(target);
        backRightEncoder.runToPosition(target);
    }

    public void driveToPosition(int target, double speed) {
        target *= PULSES_PER_ROTATION / WHEEL_DIAMETER; // convert from inches -> ticks

        frontLeftEncoder.runToPosition(target, speed);
        backLeftEncoder.runToPosition(target, speed);
        frontRightEncoder.runToPosition(target, speed);
        backRightEncoder.runToPosition(target, speed);
    }

    @Override
    public void initialize() {
        gyro.init();
        reset();
    }

    @Override
    public void reset() {
        gyro.reset();

        backRightEncoder.resetEncoderCount();
        frontRightEncoder.resetEncoderCount();
        backLeftEncoder.resetEncoderCount();
        frontLeftEncoder.resetEncoderCount();
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
