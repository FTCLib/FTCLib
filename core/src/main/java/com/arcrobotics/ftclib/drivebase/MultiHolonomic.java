package org.firstinspires.ftc.teamcode;

import com.arcrobotics.ftclib.drivebase.RobotDrive;
import com.arcrobotics.ftclib.geometry.Vector2d;
import com.arcrobotics.ftclib.hardware.motors.Motor;

import java.util.ArrayList;
import java.util.Arrays;

public class MultiHolonomic extends RobotDrive {
    private Motor[] motors;
    private ArrayList<Double> angles = new ArrayList<>();

    public MultiHolonomic(Motor[] motors, double[] angles) {
        this.motors=motors;
        for (double d : angles) {
            this.angles.add(d);
        }
    }

    public MultiHolonomic(Motor[] motors) {
        this.motors=motors;
        for (int i=0;i<motors.length;i++) {
            this.angles.add(Math.toRadians(360/motors.length*i));
        }
    }
    public MultiHolonomic(Motor[] motors, double offset) {
        this.motors=motors;
        for (int i=0;i<motors.length;i++) {
            this.angles.add(Math.toRadians((360/motors.length*i+offset)));
        }
    }

    public void setMaxSpeed(double value) {
        super.setMaxSpeed(value);
    }
    public void setRange(double min, double max) {
        super.setRange(min, max);
    }

    public void driveFieldCentric(double strafeSpeed, double forwardSpeed, double turn, double heading) {
        strafeSpeed = clipRange(strafeSpeed);
        forwardSpeed = clipRange(forwardSpeed);
        turn = clipRange(turn);

        Vector2d vector = new Vector2d(strafeSpeed, forwardSpeed);
        vector = vector.rotateBy(-heading);
        double theta = vector.angle();

        double[] speeds = new double[motors.length];

        for (int i=0;i<motors.length;i++) {
            speeds[i] = Math.cos(theta+angles.get(i));


        }
        normalize(speeds,vector.magnitude());
        for (int i=0;i<motors.length;i++) {
            motors[i].set(speeds[i] * maxOutput + maxOutput*turn/2);
        }
    }
    public void driveRobotCentric(double strafeSpeed, double forwardSpeed, double turn) {
        driveFieldCentric(strafeSpeed, forwardSpeed, turn, 0.0);
    }



    @Override
    public void stop() {
        for (Motor m : motors) {
            m.stopMotor();
        }
    }
}
