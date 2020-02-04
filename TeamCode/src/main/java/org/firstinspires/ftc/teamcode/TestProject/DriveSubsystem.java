package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

import com.arcrobotics.ftclib.command.Subsystem;
import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.gamepad.ButtonReader;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.hardware.RevIMU;
import com.arcrobotics.ftclib.hardware.motors.SimpleMotorImpl;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class DriveSubsystem implements Subsystem {

    GamepadEx driverGamepad;
    Telemetry telemetry;

    //Gyro
    RevIMU gyro;

    public SimpleMotorImpl backLeftMotor, frontLeftMotor, backRightMotor, frontRightMotor;
    public MecanumDrive driveTrain;

    final double WHEEL_DIAMETER = 4; // Inches
    final int PULSES_PER_ROTATION = 280; // NEVEREST 40


    ButtonReader slowDownButton;


    public DriveSubsystem(GamepadEx driverGamepad, HardwareMap hw, Telemetry telemetry) {
        this.driverGamepad = driverGamepad;
        this.telemetry = telemetry;

        backLeftMotor = new SimpleMotorImpl(hw, telemetry,"backLeftMotor", 383.6);
        frontLeftMotor = new SimpleMotorImpl(hw, telemetry,"frontLeftMotor", 383.6);
        backRightMotor = new SimpleMotorImpl(hw, telemetry, "backRightMotor", 383.6);
        frontRightMotor = new SimpleMotorImpl(hw, telemetry, "frontRightMotor", 383.6);

        gyro = new RevIMU(hw);


        backLeftMotor.setInverted(true);
        frontLeftMotor.setInverted(true);

        backLeftMotor.setCpr(PULSES_PER_ROTATION);
        frontLeftMotor.setCpr(PULSES_PER_ROTATION);
        backRightMotor.setCpr(PULSES_PER_ROTATION);
        frontRightMotor.setCpr(PULSES_PER_ROTATION);


        driveTrain = new MecanumDrive(false, frontLeftMotor, frontRightMotor, backLeftMotor, backRightMotor);

        slowDownButton = new ButtonReader(driverGamepad, GamepadKeys.Button.X);
    }

    public double getHeading() {
        return gyro.getHeading();
    }

    @Override
    public void initialize() {
        gyro.init();
        reset();
    }

    @Override
    public void reset() {
        backLeftMotor.reset();
        backRightMotor.reset();
        frontLeftMotor.reset();
        frontRightMotor.reset();
        gyro.reset();

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
