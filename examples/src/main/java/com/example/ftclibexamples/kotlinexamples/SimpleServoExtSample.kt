package com.example.ftclibexamples.kotlinexamples

import com.arcrobotics.ftclib.hardware.SimpleServo
import com.arcrobotics.ftclib.kotlin.extensions.hardware.range
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode

class SimpleServoExtSample : LinearOpMode() {
    override fun runOpMode() {
        val servo = SimpleServo(hardwareMap, "onlyServo", 0.0, 180.0) // range default to 0 to 180

        telemetry.addData("Current Range", servo.range)

        servo.range = (-90) to (90) // setting new range to -90 to 90

        telemetry.addData("New Range", servo.range)

        telemetry.update()

        waitForStart()
    }
}