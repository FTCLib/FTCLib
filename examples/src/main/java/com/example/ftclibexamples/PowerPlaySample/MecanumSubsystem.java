package com.example.ftclibexamples.PowerPlaySample;

import com.arcrobotics.ftclib.command.Command;
import com.arcrobotics.ftclib.command.RunCommand;
import com.arcrobotics.ftclib.command.SubsystemBase;
import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.hardware.RevIMU;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;

import java.util.function.DoubleSupplier;

public class MecanumSubsystem extends SubsystemBase {
    private final MecanumDrive drive;

    public MecanumSubsystem(MotorEx frontLeft, MotorEx frontRight, MotorEx backLeft, MotorEx backRight) {
        drive = new MecanumDrive(frontLeft, frontRight, backLeft, backRight);
    }

    public Command fieldCentric(DoubleSupplier strafeSpeed, DoubleSupplier forwardSpeed,
                                DoubleSupplier turnSpeed, DoubleSupplier gyroAngle){
        return new RunCommand(
                () -> drive.driveFieldCentric(strafeSpeed.getAsDouble(), forwardSpeed.getAsDouble(),
                        turnSpeed.getAsDouble(), gyroAngle.getAsDouble()),
                this
        );
    }

    public Command robotCentric(DoubleSupplier strafeSpeed, DoubleSupplier forwardSpeed,
                                DoubleSupplier turnSpeed){
        return new RunCommand(
                () -> drive.driveRobotCentric(strafeSpeed.getAsDouble(), forwardSpeed.getAsDouble(),
                        turnSpeed.getAsDouble()),
                this
        );
    }
}
