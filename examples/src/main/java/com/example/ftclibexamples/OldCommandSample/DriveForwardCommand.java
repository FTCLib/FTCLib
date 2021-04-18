package com.example.ftclibexamples.OldCommandSample;

import com.arcrobotics.ftclib.command.old.Command;
import com.qualcomm.robotcore.util.ElapsedTime;

public class DriveForwardCommand implements Command {

    private DriveSubsystem driveSubsystem;
    private ElapsedTime timer;
    private double distance, speed;
    // 4 inches e.g

    public DriveForwardCommand(DriveSubsystem driveSubsystem, double distance, double speed) {
        this.driveSubsystem = driveSubsystem;
        this.distance = distance;
        this.speed = speed;
    }

    @Override
    public void initialize() {
        driveSubsystem.reset();
    }

    @Override
    public void execute() {
        driveSubsystem.driveToPosition((int) distance, speed);
    }


    @Override
    public void end() {
        driveSubsystem.reset();
        driveSubsystem.stop();
    }


    @Override
    public boolean isFinished() {
        return driveSubsystem.atTargetPos();
    }
}
