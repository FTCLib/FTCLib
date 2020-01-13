package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

import com.arcrobotics.ftclib.command.Command;
import com.arcrobotics.ftclib.controller.PController;
import com.qualcomm.robotcore.util.ElapsedTime;

public class TurnAngleCommand implements Command {

    DriveSubsystem driveSubsystem;
    double angle;

    // Proportional Controller for correcting for gyro error
    PController headingController;

    public TurnAngleCommand(DriveSubsystem driveSubsystem, double angle) {
        this.driveSubsystem = driveSubsystem;
        this.angle = angle;

        // At 180 degrees, we should spin almost as fast as we can to correct
        // 1 is full power. 180 * 0.05 = 0.9
        headingController = new PController(0.005);

    }


    @Override
    public void initialize() {
        // Reset gyro and encoders
        driveSubsystem.reset();

        // Set target to the target angle
        headingController.setSetPoint(angle);
        // If within 5 degrees of setpoint, the target is considered reached
        headingController.setTolerance(5);

    }

    @Override
    public void execute() {
        // Calculate output
        double rotate = headingController.calculate(driveSubsystem.getHeading());

        // apply output
        driveSubsystem.driveTrain.driveRobotCentric(0, rotate, 0);
    }

    @Override
    public void end() {

    }

    @Override
    public boolean isFinished() {
        boolean angleReached = headingController.atSetPoint();
        return angleReached;
    }
}
