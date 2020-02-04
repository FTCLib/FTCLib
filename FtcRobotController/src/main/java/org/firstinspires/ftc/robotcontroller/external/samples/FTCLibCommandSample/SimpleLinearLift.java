package org.firstinspires.ftc.robotcontroller.external.samples.FTCLibCommandSample;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.arcrobotics.ftclib.hardware.motors.MotorImplEx;
import com.arcrobotics.ftclib.util.Timing;

import java.util.concurrent.TimeUnit;

public class SimpleLinearLift {

    MotorImplEx m_liftMotor;
    PIDFController m_controller;

    public SimpleLinearLift(MotorImplEx liftMotor, PIDFController controller) {
        m_liftMotor = liftMotor;
        m_controller = controller;

        m_liftMotor.encoder.resetEncoderCount();
    }

    public void moveLift(double power) {
        double error = m_controller.calculate(power, m_liftMotor.getPower());
        m_liftMotor.setPower(m_liftMotor.getPower() + error);
    }

    public void moveToPosition(double desiredTicks) {
        if (m_controller.atSetPoint()) m_controller.reset();
            
        m_liftMotor.setPower(
                m_controller.calculate(desiredTicks, m_liftMotor.getEncoderPulses())
                / desiredTicks
        );
    }

    public void moveWithTimer(int activeTime) {
        if (m_controller.atSetPoint()) m_controller.reset();
        
        Timing.Timer timer = new Timing.Timer(activeTime, TimeUnit.MILLISECONDS);
        timer.start();

        while (!timer.done()) {
            m_liftMotor.setPower(m_controller.calculate(activeTime, timer.currentTime()) / activeTime);
        }
    }

    public void resetPositionCounter() {
        m_liftMotor.encoder.resetEncoderCount();
    }

}
