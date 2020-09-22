package com.example.ftclibexamples.CommandSample;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name="Sample TeleOp")
public class SampleTeleOp extends LinearOpMode {

    SampleRobot m_robot = new SampleRobot(this, true);

    @Override
    public void runOpMode() throws InterruptedException {
        waitForStart();

        while (opModeIsActive() && !isStopRequested()) {
            m_robot.run();
        }

        m_robot.reset();
    }

}