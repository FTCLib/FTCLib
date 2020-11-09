package com.example.ftclibexamples;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.command.PIDFCommand;
import com.arcrobotics.ftclib.controller.PDController;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;

@Autonomous(name="Sample PID")
@Disabled
public class PIDSample extends CommandOpMode {

    private Motor m_motor;
    private PIDFCommand m_simple, m_custom;
    private PDController m_controller;

    @Override
    public void initialize() {
        m_motor = new Motor(hardwareMap, "motor");
        m_controller = new PDController(0.1, 0.07);

        // construct a PIDFCommand using your controller
        m_simple = new PIDFCommand(
                m_controller, m_motor::getCurrentPosition,
                u -> m_motor.set(u)
        ).setSetPoint(1000);

        // construct a PIDFCommand and set the constant values
        m_custom = new PIDFCommand(m_motor::getCurrentPosition, u -> m_motor.set(u))
                .setSetPoint(1000)
                .setP(0.1)
                .setD(0.07);

        schedule(m_simple, m_custom);
    }

}
