package org.firstinspires.ftc.teamcode.TestProject;

import com.arcrobotics.ftclib.command.Subsystem;
import com.arcrobotics.ftclib.drivebase.DifferentialDrive;
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
    public DifferentialDrive driveTrain;

    final double WHEEL_DIAMETER = 4; // Inches
    final int PULSES_PER_ROTATION = 280; // NEVEREST 40


    ButtonReader slowDownButton;


    public DriveSubsystem(GamepadEx driverGamepad, HardwareMap hw, Telemetry telemetry) {
        this.driverGamepad = driverGamepad;
        this.telemetry = telemetry;

        backLeftMotor = new SimpleMotorImpl(hw, telemetry,"backLeftMotor");
        frontLeftMotor = new SimpleMotorImpl(hw, telemetry,"frontLeftMotor");
        backRightMotor = new SimpleMotorImpl(hw, telemetry, "backRightMotor");
        frontRightMotor = new SimpleMotorImpl(hw, telemetry, "frontRightMotor");

        gyro = new RevIMU(hw);


        backLeftMotor.setInverted(true);
        frontLeftMotor.setInverted(true);

        backLeftMotor.setCpr(PULSES_PER_ROTATION);
        frontLeftMotor.setCpr(PULSES_PER_ROTATION);
        backRightMotor.setCpr(PULSES_PER_ROTATION);
        frontRightMotor.setCpr(PULSES_PER_ROTATION);


        driveTrain = new DifferentialDrive(false, frontLeftMotor, frontRightMotor, backLeftMotor, backRightMotor);

        slowDownButton = new ButtonReader(driverGamepad, GamepadKeys.Button.X);
        gyro.invertGyro();
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

        driveTrain.arcadeDrive(driverGamepad.getLeftY()  * maxSpeed,
                 driverGamepad.getRightX() * maxSpeed, false);
    }

    @Override
    public void stop() {
        driveTrain.arcadeDrive(0, 0, false);
    }

    @Override
    public void disable() {
        stop();
    }
}
