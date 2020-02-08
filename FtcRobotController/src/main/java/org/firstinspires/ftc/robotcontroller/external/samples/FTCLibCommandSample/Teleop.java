package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.hardware.motors.MotorImplEx;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
@Disabled
@TeleOp(name = "Command-Based Teleop Sample", group = "Command")
public class Teleop extends OpMode {

    public static final double kP = 0.003;
    public static final double kI = 0.12;
    public static final double kD = 0.053;
    public static final double kF = 0.07;
    public static final double kThreshold = 8;

    private DriveSubsystem driveSubsystem;
    private GamepadEx driverGamepad;
    private PIDLiftController liftController;
    private SimpleLinearLift lift;
    private MotorImplEx liftMotor;

    public static PIDFController pidf = new PIDFController(
            new double[]{kP, kI, kD, kF}
    );

    @Override
    public void init() {
        driverGamepad = new GamepadEx(gamepad1);
        driveSubsystem = new DriveSubsystem(driverGamepad, hardwareMap, telemetry);

        pidf.reset();
        pidf.setTolerance(kThreshold);

        driveSubsystem.initialize();

        liftMotor = new MotorImplEx(hardwareMap, "lift", 537.6);
        lift = new SimpleLinearLift(liftMotor, pidf);
        liftController = new PIDLiftController(lift);
    }

    @Override
    public void loop() {
        driveSubsystem.loop();
        liftController.power(gamepad2.left_stick_y);

        // set the lift states through the bumpers
        if (gamepad2.a) {
            liftController.setStageOne();
        } else if (gamepad2.b) {
            liftController.setStageTwo();
        } else if (gamepad2.x) {
            liftController.setStageThree();
        } else if (gamepad2.y) {
            liftController.resetStage();
        }
    }
}
