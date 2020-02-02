package org.firstinspires.ftc.robotcontroller.external.samples.MecanumOdometrySample.FTCLibCommandSample;

import com.arcrobotics.ftclib.command.Subsystem;
import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.gamepad.ButtonReader;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.geometry.Pose2d;
import com.arcrobotics.ftclib.hardware.JSTEncoder;
import com.arcrobotics.ftclib.hardware.RevIMU;
import com.arcrobotics.ftclib.hardware.motors.SimpleMotorImpl;
import com.arcrobotics.ftclib.kinematics.HolonomicOdometry;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class DriveSubsystem implements Subsystem {

    GamepadEx driverGamepad;
    Telemetry telemetry;

    HolonomicOdometry odometry;

    //Gyro
    RevIMU gyro;

    public SimpleMotorImpl backLeftMotor, frontLeftMotor, backRightMotor, frontRightMotor;
    public MecanumDrive driveTrain;
    public JSTEncoder horizontalEncoder, leftEncoder, rightEncoder;

    final double trackWidth = 16; // Distance from center of left wheel to center of right wheel
    final double WHEEL_DIAMETER = 4; // Inches
    final int PULSES_PER_ROTATION = 280; // NEVEREST 40

    ButtonReader slowDownButton;

    public DriveSubsystem(GamepadEx driverGamepad, HardwareMap hw, Telemetry telemetry) {
        this.driverGamepad = driverGamepad;
        this.telemetry = telemetry;

        odometry = new HolonomicOdometry(trackWidth);

        backLeftMotor = new SimpleMotorImpl(hw, telemetry,"backLeftMotor");
        frontLeftMotor = new SimpleMotorImpl(hw, telemetry,"frontLeftMotor");
        backRightMotor = new SimpleMotorImpl(hw, telemetry, "backRightMotor");
        frontRightMotor = new SimpleMotorImpl(hw, telemetry, "frontRightMotor");

        horizontalEncoder = new JSTEncoder(hw, "hEncoder");
        horizontalEncoder.setDistancePerPulse((WHEEL_DIAMETER * Math.PI) / PULSES_PER_ROTATION);

        leftEncoder = new JSTEncoder(hw, "lEncoder");
        leftEncoder.setDistancePerPulse((WHEEL_DIAMETER * Math.PI) / PULSES_PER_ROTATION);

        rightEncoder = new JSTEncoder(hw, "hEncoder");
        rightEncoder.setDistancePerPulse((WHEEL_DIAMETER * Math.PI) / PULSES_PER_ROTATION);
        rightEncoder.setInverted(true);

        gyro = new RevIMU(hw);

        // Autoinverts right side
        driveTrain = new MecanumDrive(frontLeftMotor, frontRightMotor, backLeftMotor, backRightMotor);

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

    public void updateOdometry() {
        odometry.update(gyro.getAbsoluteHeading(), horizontalEncoder.getDistance(),
                leftEncoder.getDistance(), rightEncoder.getDistance());
    }

    public Pose2d getRobotPose() {
        updateOdometry();
        return odometry.robotPos;
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

        telemetry.addData("Odometry: ", getRobotPose().toString());
        telemetry.addData("Absolute Heading: ", gyro.getAbsoluteHeading());

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
