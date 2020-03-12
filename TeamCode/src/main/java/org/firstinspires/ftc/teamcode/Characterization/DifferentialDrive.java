package org.firstinspires.ftc.teamcode.Characterization;

import com.arcrobotics.ftclib.command.Command;
import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.hardware.JSTEncoder;
import com.arcrobotics.ftclib.hardware.motors.SimpleMotorImpl;
import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.ArrayList;

public class DifferentialDrive extends CommandOpMode {
    final double WHEEL_DIAMETER = .3333;
    final int EPR = 360;
    SimpleMotorImpl frontLeftMotor, frontRightMotor, backLeftMotor, backRightMotor;
    JSTEncoder leftEncoder, rightEncoder;

    @Override
    public void initialize() {
        frontLeftMotor = new SimpleMotorImpl(hardwareMap, telemetry, "frontLeft", EPR);
        frontRightMotor = new SimpleMotorImpl(hardwareMap, telemetry, "frontRight", EPR);
        backLeftMotor = new SimpleMotorImpl(hardwareMap, telemetry, "rearLeft", EPR);
        backRightMotor = new SimpleMotorImpl(hardwareMap, telemetry, "rearRight", EPR);

        frontLeftMotor.setInverted(true);
        backLeftMotor.setInverted(true);
        frontRightMotor.setInverted(false);
        backRightMotor.setInverted(false);

        leftEncoder = new JSTEncoder(hardwareMap, "leftEncoder");
        rightEncoder = new JSTEncoder(hardwareMap, "rightEncoder");
        leftEncoder.setDistancePerPulse(0.001);
        rightEncoder.setDistancePerPulse(0.001);
    }

    @Override
    public void run() {
        ElapsedTime timer = new ElapsedTime();


        ArrayList<double[]> quasiStaticKvTest = new ArrayList<>();
        double autoSpeed = 0;
        double ramp = 3;
        timer.reset();
        leftEncoder.syncEncoder();

        double pastTime = 0;
        double pastDistance = 0;

        // change in position / change in time
        // (current pos - initial pos) / change in time
        while(timer.seconds() < 5) {
            quasiStaticKvTest.add(new double[] {(leftEncoder.getDistance() - pastDistance) / (timer.seconds() - pastTime), autoSpeed, timer.seconds()});
            pastTime = timer.seconds();
            pastDistance = leftEncoder.getDistance();
            setDriveVolts(autoSpeed);
            autoSpeed += (ramp * 0.05) / 12;
            sleep(50);
        }
        setDriveVolts(0);
        sleep(500);
        ArrayList<double[]> kATest = new ArrayList<>();
        autoSpeed = 8;
        timer.reset();
        // reset
        leftEncoder.syncEncoder();

        pastTime = 0;
        pastDistance = 0;

        while(timer.seconds() < 3) {

            double deltaTime = timer.seconds() - pastTime;
            kATest.add(new double[] {(leftEncoder.getDistance() - pastDistance) / (Math.pow(deltaTime, 2)), autoSpeed, timer.seconds()});
            pastTime = timer.seconds();
            pastDistance = leftEncoder.getDistance();
            setDriveVolts(autoSpeed);
            sleep(50);
        }

    }



    private void setDriveVolts(double volts) {
        frontRightMotor.setVoltage(volts);
        frontLeftMotor.setVoltage(volts);
        backRightMotor.setVoltage(volts);
        backLeftMotor.setVoltage(volts);
    }
}
