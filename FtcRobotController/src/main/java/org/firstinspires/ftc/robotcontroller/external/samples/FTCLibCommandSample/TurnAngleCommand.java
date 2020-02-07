package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

import com.arcrobotics.ftclib.command.Command;
import com.arcrobotics.ftclib.controller.PController;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class TurnAngleCommand implements Command {

    DriveSubsystem driveSubsystem;
    Telemetry tl;
    double angle;

    // Proportional Controller for correcting for gyro error
    PController headingController;

    public TurnAngleCommand(DriveSubsystem driveSubsystem, double angle, Telemetry telemetry) {
        this.driveSubsystem = driveSubsystem;
        this.angle = angle;
        this.tl = telemetry;
        // At 180 degrees, we should spin almost as fast as we can to correct
        // 1 is full power. 180 * 0.05 = 0.9
        headingController = new PController(0.05, angle, driveSubsystem.getHeading(), 0.02);
        headingController.setSetPoint(angle);

    }


    @Override
    public void initialize() {
        // Reset gyro and encoders
        driveSubsystem.reset();
        driveSubsystem.frontLeftMotor.setRunMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        driveSubsystem.frontRightMotor.setRunMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        driveSubsystem.backLeftMotor.setRunMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        driveSubsystem.backRightMotor.setRunMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // Set target to the target angle
        tl.addData("Heading Setpoint", headingController.getSetPoint());
        // If within 5 degrees of setpoint, the target is considered reached
        headingController.setTolerance(1);

    }

    @Override
    public void execute() {
        // Calculate output
        double rotate = headingController.calculate(driveSubsystem.getHeading());

        // apply output
        driveSubsystem.driveTrain.driveRobotCentric(0, 0, rotate);
    }

    @Override
    public void end() {
        driveSubsystem.driveTrain.driveRobotCentric(0, 0, 0);

    }

    @Override
    public boolean isFinished() {
        tl.addData("Position Error: ", headingController.getPositionError());
        tl.addData("Heading Setpoint", headingController.getSetPoint());
        tl.addData("At Setpoint", headingController.atSetPoint());
        tl.addData("Current heading", driveSubsystem.getHeading());
        boolean angleReached = headingController.atSetPoint();
        return angleReached;
    }
}
