package org.firstinspires.ftc.robotcontroller.external.samples;

import com.arcrobotics.ftclib.drivebase.swerve.DiffySwerveDrive;
import com.arcrobotics.ftclib.drivebase.swerve.DiffySwerveModuleEx;
import com.arcrobotics.ftclib.hardware.motors.MotorImplEx;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name="Diffy Swerve Sample 2")
@Disabled
public class DifferentialSwerveSampleV2 extends LinearOpMode {

    public final double TICKS_PER_MODULE_REV = 28 * (double)(60)/11 *
            (double)(48)/15 * (double)(82)/22 * 2; //ticks per MODULE revolution

    public final double DEGREES_PER_TICK = 360/TICKS_PER_MODULE_REV;

    public final double TICKS_PER_WHEEL_REV = 28 * (double)(60)/11 *
            (double)(48)/15 * (double)(82)/22 * (double)(14)/60; //ticks per WHEEL revolution

    public final double CM_WHEEL_DIAMETER = 3 * 2.54;
    public final double CM_PER_WHEEL_REV = CM_WHEEL_DIAMETER * Math.PI;
    public final double CM_PER_TICK = CM_PER_WHEEL_REV/TICKS_PER_WHEEL_REV;

    private MotorImplEx moduleOne_LEFT, moduleOne_RIGHT, moduleTwo_LEFT, moduleTwo_RIGHT;
    private DiffySwerveModuleEx left, right;
    private DiffySwerveDrive driveTrain;

    @Override
    public void runOpMode() throws InterruptedException {

        moduleOne_LEFT = new MotorImplEx(hardwareMap, "oneLeft", 383.6);
        moduleOne_RIGHT = new MotorImplEx(hardwareMap, "oneRight", 383.6);
        moduleTwo_LEFT = new MotorImplEx(hardwareMap, "twoLeft", 383.6);
        moduleTwo_RIGHT = new MotorImplEx(hardwareMap, "twoRight", 383.6);

        left = new DiffySwerveModuleEx(moduleOne_LEFT, moduleOne_RIGHT);
        right = new DiffySwerveModuleEx(moduleTwo_LEFT, moduleTwo_RIGHT);

        driveTrain = new DiffySwerveDrive(
                left, right
        );

        waitForStart();

        // TODO: uh, I think this will work, but someone should test

        while (opModeIsActive() && !isStopRequested()) {

            driveTrain.drive(
                    gamepad1.left_stick_x,
                    gamepad1.left_stick_y,
                    gamepad1.right_stick_x,
                    gamepad1.right_stick_y
            );

            left.updateTracking();
            right.updateTracking();

            telemetry.addData("Left Side Distance", left.getDistanceTravelled());
            telemetry.addData("Right Side Distance", right.getDistanceTravelled());
        }

    }

}
