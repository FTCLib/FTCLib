package org.firstinspires.ftc.robotcontroller.external.samples.CommandSample;

import com.arcrobotics.ftclib.Robot;
import com.arcrobotics.ftclib.command.button.GamepadButton;
import com.arcrobotics.ftclib.hardware.motors.SimpleMotorEx;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name="Sample TeleOp")
public class SampleTeleOp extends LinearOpMode {

    SampleRobot m_robot = new SampleRobot(this);

    @Override
    public void runOpMode() throws InterruptedException {
        m_robot.initTeleOp();

        waitForStart();

        while (opModeIsActive() && !isStopRequested()) {
            m_robot.run();
        }

        m_robot.reset();
    }

}