package org.firstinspires.ftc.teamcode;

import com.arcrobotics.ftclib.drivebase.MecanumDrive;
import com.arcrobotics.ftclib.hardware.motors.MotorImpl;
import com.arcrobotics.ftclib.hardware.motors.MotorImplEx;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name="Sample OpMode #1")
public class SampleTeleOp extends OpMode {

    /**
     * The number of ticks per revolution in the motors of the drivetrain.
     * The motors being used here are the goBILDA Yellow Jackets, 1150 rpm.
     */
    private static final double CPR_DRIVETRAIN = 145.6;

    /**
     * The implemented motors. These will hold the custom motors and the methods within
     * for our custom drivetrain.
     */
    private MotorImpl tfL, tfR, tbL, tbR;
    private MotorImplEx fL, fR, bL, bR;
    private MecanumDrive dt;

    @Override
    public void init() {
        tfL = new MotorImpl(hardwareMap, "frontLeft", CPR_DRIVETRAIN);
        tfR = new MotorImpl(hardwareMap, "frontRight", CPR_DRIVETRAIN);
        tbL = new MotorImpl(hardwareMap, "backLeft", CPR_DRIVETRAIN);
        tbR = new MotorImpl(hardwareMap, "backRight", CPR_DRIVETRAIN);

        fL = new MotorImplEx(tfL);
        fR = new MotorImplEx(tfR);
        bL = new MotorImplEx(tbL);
        bR = new MotorImplEx(tbR);

        dt = new MecanumDrive(fL, fR, bL, bR);
    }

    @Override
    public void loop() {
        dt.driveRobotCentric(gamepad1.left_stick_x, gamepad1.left_stick_y, gamepad1.right_stick_x);
    }

    public void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

}