package com.example.ftclibexamples;

import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.hardware.RevIMU;
import com.arcrobotics.ftclib.hardware.SimpleServo;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.util.TimedAction;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

/**
 * This sample makes use of field-centric mecanum drive and
 * an {@link TimedAction} to create a flicker servo mechanism
 * on a drivebase.
 */
@TeleOp
@Disabled
public class FlickerServoSample extends LinearOpMode {

    private SimpleServo flickerServo;
    private MecanumDrive drive;
    private RevIMU imu;
    private TimedAction flicker;
    private GamepadEx driverOp;

    @Override
    public void runOpMode() throws InterruptedException {
        drive = new MecanumDrive(
                new Motor(hardwareMap, "front_left"),
                new Motor(hardwareMap, "front_right"),
                new Motor(hardwareMap, "back_left"),
                new Motor(hardwareMap, "back_right")
        );

        imu = new RevIMU(hardwareMap);
        imu.init();

        flickerServo = new SimpleServo(hardwareMap, "flicker", 0, 270);
        // for this example, 35 degrees is the resting position and 80 degrees is the flick out position
        flicker = new TimedAction(
                () -> flickerServo.turnToAngle(35),
                () -> flickerServo.turnToAngle(80),
                500,    // 500 ms between flick positions
                true
        );

        driverOp = new GamepadEx(gamepad1);

        waitForStart();

        while (isStarted() && !isStopRequested()) {
            drive.driveFieldCentric(
                    driverOp.getLeftX(),
                    driverOp.getLeftY(),
                    driverOp.getRightX(),
                    imu.getHeading(),    // returns degrees
                    true    // square inputs
            );

            if (driverOp.isDown(GamepadKeys.Button.A) && flicker.running()) {
                flicker.reset();
            }
            flicker.run();

            driverOp.readButtons();
        }
    }

}
