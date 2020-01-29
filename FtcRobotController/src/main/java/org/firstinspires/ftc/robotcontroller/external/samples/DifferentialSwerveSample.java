package org.firstinspires.ftc.robotcontroller.external.samples;

import com.arcrobotics.ftclib.drivebase.swerve.DiffySwerveDrive;
import com.arcrobotics.ftclib.drivebase.swerve.DiffySwerveModule;
import com.arcrobotics.ftclib.hardware.motors.MotorImplEx;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name="Differential Swerve Sample")
@Disabled
public class DifferentialSwerveSample extends LinearOpMode {

    private DiffySwerveDrive driveTrain;
    private MotorImplEx moduleOne_LEFT, moduleOne_RIGHT, moduleTwo_LEFT, moduleTwo_RIGHT;

    @Override
    public void runOpMode() throws InterruptedException {

        moduleOne_LEFT = new MotorImplEx(hardwareMap, "oneLeft", 383.6);
        moduleOne_RIGHT = new MotorImplEx(hardwareMap, "oneRight", 383.6);
        moduleTwo_LEFT = new MotorImplEx(hardwareMap, "twoLeft", 383.6);
        moduleTwo_RIGHT = new MotorImplEx(hardwareMap, "twoRight", 383.6);

        driveTrain = new DiffySwerveDrive(
                    new DiffySwerveModule(moduleOne_LEFT, moduleOne_RIGHT),
                    new DiffySwerveModule(moduleTwo_LEFT, moduleTwo_RIGHT));

        waitForStart();

        while (opModeIsActive() && !isStopRequested()) {

            driveTrain.drive(
                    gamepad1.left_stick_x,
                    gamepad1.left_stick_y,
                    gamepad1.right_stick_x,
                    gamepad1.right_stick_y
            );

        }
    }

}
