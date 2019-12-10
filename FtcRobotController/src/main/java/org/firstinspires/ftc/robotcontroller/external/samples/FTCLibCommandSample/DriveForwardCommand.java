package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

import com.arcrobotics.ftclib.command.Command;
import com.qualcomm.robotcore.util.ElapsedTime;

public class DriveForwardCommand implements Command {

    private DriveSubsystem driveSubsystem;
    private ElapsedTime timer;
    private double distance, speed, timeout;

    public DriveForwardCommand(DriveSubsystem driveSubsystem, double distance, double speed, double timeout) {
        this.driveSubsystem = driveSubsystem;
        this.distance = distance;
        this.speed = speed;
        this.timeout = timeout;

        timer = new ElapsedTime();
    }

    @Override
    public void initialize() {
        try {
            driveSubsystem.backLeftMotor.setTargetDistance(distance);
        } catch(Exception e) {
            e.printStackTrace();
        }
        timer.startTime();
    }

    @Override
    public void execute() {
        driveSubsystem.driveTrain.driveRobotCentric(0, speed, 0);
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
        boolean timeoutReached = timer.seconds() >= timeout;

        return distanceReached || timeoutReached;
    }
}
