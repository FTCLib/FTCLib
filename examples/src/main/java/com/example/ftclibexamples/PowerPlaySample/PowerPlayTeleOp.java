package com.example.ftclibexamples.PowerPlaySample;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.gamepad.GamepadEx;

import com.arcrobotics.ftclib.hardware.RevIMU;
import com.arcrobotics.ftclib.hardware.SimpleServo;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;

import static com.arcrobotics.ftclib.gamepad.GamepadKeys.Button.*;

/**
 * A TeleOp for a bot with a servo powered claw, a vertical slide powered by two motors,
 * and a mecanum drive.
 *
 */
public class PowerPlayTeleOp extends CommandOpMode {

    private MotorEx frontLeft, frontRight, backLeft, backRight, liftLeft, liftRight;
    private SimpleServo clawServo;
    private RevIMU imu;

    private ClawSubsystem claw;
    private LiftSubsystem lift;
    private MecanumSubsystem drive;

    private GamepadEx gamepadEx1, gamepadEx2;


    @Override
    public void initialize() {
        gamepadEx1 = new GamepadEx(gamepad1);
        gamepadEx2 = new GamepadEx(gamepad2);

        initHardware();
        setUpHardware();

        claw = new ClawSubsystem(clawServo);
        lift = new LiftSubsystem(liftLeft, liftRight);
        drive = new MecanumSubsystem(frontLeft, frontRight, backLeft, backRight);

        telemetry.addData("Mode", "Done initializing");
        telemetry.update();

        gamepadEx2.getGamepadButton(RIGHT_BUMPER)
                .toggleWhenPressed(claw.runGrabCommand(), claw.runReleaseCommand());
        gamepadEx2.getGamepadButton(A).whenPressed(lift.goTo(Junction.NONE));
        gamepadEx2.getGamepadButton(DPAD_DOWN).whenPressed(lift.goTo(Junction.GROUND));
        gamepadEx2.getGamepadButton(X).whenPressed(lift.goTo(Junction.LOW));
        gamepadEx2.getGamepadButton(B).whenPressed(lift.goTo(Junction.MEDIUM));
        gamepadEx2.getGamepadButton(Y).whenPressed(lift.goTo(Junction.HIGH));



        register(claw, lift, drive);
        drive.setDefaultCommand(drive.robotCentric(gamepadEx1::getLeftX,
                gamepadEx1::getLeftY, gamepadEx1::getRightX));
    }


    /**
     * Instantiate hardware devices
     */
    private void initHardware() {
        frontLeft = new MotorEx(hardwareMap, "frontLeft");
        frontRight = new MotorEx(hardwareMap, "frontRight");
        backLeft = new MotorEx(hardwareMap, "backLeft");
        backRight = new MotorEx(hardwareMap, "backRight");

        liftLeft = new MotorEx(hardwareMap, "liftLeft");
        liftRight = new MotorEx(hardwareMap, "liftRight");

        clawServo = new SimpleServo(hardwareMap, "claw", 0, 120);

        imu = new RevIMU(hardwareMap);
        imu.init();
    }

    /**
     * Reverse any motors, set behaviors, etc
     */
    private void setUpHardware(){
        frontLeft.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);

        liftLeft.setRunMode(Motor.RunMode.RawPower);
        liftRight.setRunMode(Motor.RunMode.RawPower);
        liftLeft.resetEncoder();
        liftRight.resetEncoder();
    }

    /**
     * Update telemetry
     */
    @Override
    public void run() {
        super.run();
        telemetry.addData("frontLeft Power", frontLeft.motor.getPower());
        telemetry.addData("frontRight Power", frontRight.motor.getPower());
        telemetry.addData("backLeft Power", backLeft.motor.getPower());
        telemetry.addData("backRight Power", backRight.motor.getPower());

        telemetry.addData("liftLeft Power", liftLeft.motor.getPower());
        telemetry.addData("liftRight Power", liftRight.motor.getPower());
        telemetry.addData("liftLeft Position", liftLeft.getCurrentPosition());
        telemetry.addData("liftRight Position", liftRight.getCurrentPosition());

        telemetry.addData("Servo Position", clawServo.getPosition());
        telemetry.addData("Current Junction", lift.getCurrentTarget());

        telemetry.update();
    }
}
