package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.controller.PIDFController;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.hardware.SimpleServo;
import com.arcrobotics.ftclib.hardware.motors.MotorImplEx;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;

@Disabled
@com.qualcomm.robotcore.eventloop.opmode.Autonomous(name="Command-based Autonomous Sample")  // @Autonomous(...) is the other common choice
public class Auto extends CommandOpMode {

    DriveSubsystem driveSubsystem;
    SimpleServo scoringServo;
    GamepadEx driverGamepad;

    private PIDLiftController liftController;
    private SimpleLinearLift lift;
    private MotorImplEx liftMotor;

    @Override
    public void initialize() {
        driverGamepad = new GamepadEx(gamepad1);
        driveSubsystem = new DriveSubsystem(driverGamepad, hardwareMap, telemetry);
        scoringServo = new SimpleServo(hardwareMap, "scoringServo");

        Teleop.pidf.reset();
        Teleop.pidf.setTolerance(Teleop.kThreshold);

        liftMotor = new MotorImplEx(hardwareMap, "lift", 537.6);
        lift = new SimpleLinearLift(
                liftMotor, Teleop.pidf
        );
        liftController = new PIDLiftController(lift);

        driveSubsystem.initialize();
    }

    @Override
    public void run() {
        // Drive Forward for 10 inches with a timeout of 4 seconds.
        addSequential(new DriveForwardCommand(driveSubsystem, 10, 0.5), 2);
        // Turn 90 degrees with a timeout of 2 seconds
        addSequential(new TurnAngleCommand(driveSubsystem, 90, telemetry), 2);
        // Rotate servo 90 degrees more than it was.
        scoringServo.rotateDegrees(90);
        // Wait for the servo to complete its action
        sleep(500);
        // Drive Forward for -10 inches with a timeout of 4 seconds.
        addSequential(new DriveForwardCommand(driveSubsystem, -10, 0.5), 4);
        while (!lift.m_controller.atSetPoint()) {
            liftController.setStageTwo();
        }
        addSequential(new DriveForwardCommand(driveSubsystem, 10, 0.75), 3);
        while (!lift.m_controller.atSetPoint()) {
            liftController.setStageOne();
        }
        scoringServo.rotateDegrees(-90);
    }
}
