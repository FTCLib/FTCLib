package com.example.ftclibexamples;

import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.hardware.motors.MotorGroup;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.util.List;

/**
 * A sample opmode for a flywheel with two motors
 * that are linked mechanically.
 */
@TeleOp
@Disabled
public class FlywheelSample extends LinearOpMode {

    private GamepadEx toolOp;
    private MotorGroup flywheel;

    public static double kP = 20;
    public static double kV = 0.7;

    @Override
    public void runOpMode() throws InterruptedException {
        toolOp = new GamepadEx(gamepad2);

        flywheel = new MotorGroup(
                new Motor(hardwareMap, "flywheel_left", Motor.GoBILDA.BARE),
                new Motor(hardwareMap, "flywheel_right", Motor.GoBILDA.BARE)
        );

        flywheel.setRunMode(Motor.RunMode.VelocityControl);
        flywheel.setVeloCoefficients(kP, 0, 0);
        flywheel.setFeedforwardCoefficients(0, kV);

        List<LynxModule> hubs = hardwareMap.getAll(LynxModule.class);
        hubs.forEach(hub -> hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL));

        waitForStart();

        while (!isStopRequested() && opModeIsActive()) {
            hubs.forEach(LynxModule::clearBulkCache);

            if (toolOp.isDown(GamepadKeys.Button.A)) {
                flywheel.set(1);
            } else {
                flywheel.stopMotor();
            }

            List<Double> velocities = flywheel.getVelocities();
            telemetry.addData("Left Flywheel Velocity", velocities.get(0));
            telemetry.addData("Right Flywheel Velocity", velocities.get(1));

            toolOp.readButtons();
        }
    }

}
