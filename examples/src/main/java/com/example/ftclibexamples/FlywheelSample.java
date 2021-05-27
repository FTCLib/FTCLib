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

    // this is our flywheel motor group
    private MotorGroup flywheel;

    public static double kP = 20;
    public static double kV = 0.7;

    @Override
    public void runOpMode() throws InterruptedException {
        toolOp = new GamepadEx(gamepad2);

        // this creates a group of two 6k RPM goBILDA motors
        // the 'flywheel_left' motor in the configuration will be set
        // as the leader for the group
        flywheel = new MotorGroup(
                new Motor(hardwareMap, "flywheel_left", Motor.GoBILDA.BARE),
                new Motor(hardwareMap, "flywheel_right", Motor.GoBILDA.BARE)
        );

        flywheel.setRunMode(Motor.RunMode.VelocityControl);
        flywheel.setVeloCoefficients(kP, 0, 0);
        flywheel.setFeedforwardCoefficients(0, kV);

        // this is not required for this example
        // here, we are setting the bulk caching mode to manual so all hardware reads
        // for the motors can be read in one hardware call.
        // we do this in order to decrease our loop time
        List<LynxModule> hubs = hardwareMap.getAll(LynxModule.class);
        hubs.forEach(hub -> hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL));

        waitForStart();

        while (!isStopRequested() && opModeIsActive()) {
            // This clears the cache for the hardware
            // Refer to https://gm0.org/en/latest/docs/software/control-system-internals.html#bulk-reads
            // for more information on bulk reads.
            hubs.forEach(LynxModule::clearBulkCache);

            if (toolOp.isDown(GamepadKeys.Button.A)) {
                flywheel.set(1);
            } else {
                flywheel.stopMotor();
            }

            // we can obtain a list of velocities with each item in the list
            // representing the motor passed in as an input to the constructor.
            // so, our flywheel_left is index 0 and flywheel_right is index 1
            List<Double> velocities = flywheel.getVelocities();
            telemetry.addData("Left Flywheel Velocity", velocities.get(0));
            telemetry.addData("Right Flywheel Velocity", velocities.get(1));

            toolOp.readButtons();
        }
    }

}
