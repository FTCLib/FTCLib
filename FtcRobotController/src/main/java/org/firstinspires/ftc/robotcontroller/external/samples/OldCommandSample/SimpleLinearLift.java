package org.firstinspires.ftc.robotcontroller.external.samples.OldCommandSample;

import com.arcrobotics.ftclib.hardware.motors.EncoderEx;
import com.arcrobotics.ftclib.hardware.motors.SimpleMotorEx;
import com.arcrobotics.ftclib.util.Direction;
import com.arcrobotics.ftclib.util.Timing;

import java.util.concurrent.TimeUnit;

public class SimpleLinearLift {

    SimpleMotorEx m_liftMotor;
    EncoderEx m_encoder;

    public SimpleLinearLift(SimpleMotorEx liftMotor) {
        m_liftMotor = liftMotor;
        m_encoder = new EncoderEx(liftMotor);

        resetPositionCounter();
    }

    public void moveLift(double power) {
        m_liftMotor.pidWrite(power);
    }

    public void moveToPosition(int position) {
        m_encoder.runToPosition(position);
    }

    public void moveWithTimer(int activeTime, Direction direction) {
        Timing.Timer timer = new Timing.Timer(activeTime, TimeUnit.MILLISECONDS);
        timer.start();

        int multiplier = direction == Direction.UP ? 1 : -1;

        while (!timer.done()) {
            m_liftMotor.pidWrite(multiplier * (activeTime - timer.currentTime()) / activeTime);
        }
    }

    public void resetPositionCounter() {
        m_encoder.resetEncoderCount();
    }

}
