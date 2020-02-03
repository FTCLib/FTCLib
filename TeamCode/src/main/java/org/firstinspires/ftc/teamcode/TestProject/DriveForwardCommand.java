package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

import com.arcrobotics.ftclib.command.Command;
import com.qualcomm.robotcore.util.ElapsedTime;

public class DriveForwardCommand implements Command {

    private DriveSubsystem driveSubsystem;
    private ElapsedTime timer;
    private double distance, speed;
    // 4 inches e.g
    private double wheelDiameter = 4;

    public DriveForwardCommand(DriveSubsystem driveSubsystem, double distance, double speed) {
        this.driveSubsystem = driveSubsystem;
        this.distance = distance;
        this.speed = speed;

    }

    @Override
    public void initialize() {
        driveSubsystem.backLeftMotor.setDistance(distance, wheelDiameter);
        driveSubsystem.backRightMotor.setDistance(distance, wheelDiameter);
        driveSubsystem.frontLeftMotor.setDistance(distance, wheelDiameter);
        driveSubsystem.frontRightMotor.setDistance(distance, wheelDiameter);
    }

    @Override
    public void execute() {
        driveSubsystem.driveTrain.driveRobotCentric(speed, 0, 0);
    }


    @Override
    public void end() {
        driveSubsystem.reset();
        driveSubsystem.stop();

    }


    @Override
    public boolean isFinished() {
        // If the robot has traveled the correct distance
        boolean distanceReached = !driveSubsystem.backLeftMotor.isBusy() && !driveSubsystem.backRightMotor.isBusy()
                && !driveSubsystem.frontLeftMotor.isBusy() && !driveSubsystem.frontRightMotor.isBusy();
        // If the timeout has been reached

        return distanceReached;
    }
}
