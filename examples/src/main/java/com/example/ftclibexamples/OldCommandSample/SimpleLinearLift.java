package com.example.ftclibexamples.OldCommandSample;

import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.arcrobotics.ftclib.util.Direction;
import com.arcrobotics.ftclib.util.Timing;

import java.util.concurrent.TimeUnit;

public class SimpleLinearLift {

    MotorEx m_liftMotor;

    public SimpleLinearLift(MotorEx liftMotor) {
        m_liftMotor = liftMotor;
        m_liftMotor.setRunMode(MotorEx.RunMode.PositionControl);

        resetPositionCounter();
    }

    public void moveLift(double power) {
        m_liftMotor.setRunMode(MotorEx.RunMode.VelocityControl);
        m_liftMotor.set(power);
    }

    public void moveToPosition(int position) {
        m_liftMotor.setTargetPosition(position);
        m_liftMotor.setRunMode(MotorEx.RunMode.PositionControl);
        m_liftMotor.set(position);
    }

    public void moveWithTimer(int activeTime, Direction direction) {
        Timing.Timer timer = new Timing.Timer(activeTime, TimeUnit.MILLISECONDS);
        timer.start();

        int multiplier = direction == Direction.UP ? 1 : -1;

        while (!timer.done()) {
            m_liftMotor.set(multiplier * (activeTime - timer.elapsedTime()) / (double) activeTime);
        }
    }

    public void resetPositionCounter() {
        m_liftMotor.resetEncoder();
    }

}
