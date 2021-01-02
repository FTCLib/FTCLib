package com.example.ftclibexamples.kotlinexamples

import com.arcrobotics.ftclib.command.CommandScheduler
import com.arcrobotics.ftclib.gamepad.GamepadEx
import com.arcrobotics.ftclib.gamepad.GamepadKeys
import com.arcrobotics.ftclib.hardware.SimpleServo
import com.arcrobotics.ftclib.kotlin.extensions.gamepad.and
import com.arcrobotics.ftclib.kotlin.extensions.gamepad.not
import com.arcrobotics.ftclib.kotlin.extensions.gamepad.whenActive
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp

@TeleOp(name = "Simple GamepadExExt Example")
@Disabled
class GamepadExExtSamples : LinearOpMode() {

    /**
     * We want to use the tool op gamepad to set the servo
     * to position 1 when the 'A' button is pressed, then
     * set it to position 0 when 'B' is pressed
     */
    private val toolOp = GamepadEx(gamepad2)
    private val grip = SimpleServo(hardwareMap, "gripper", 0.0, 270.0)

    override fun runOpMode() {
        toolOp.getGamepadButton(GamepadKeys.Button.A) and !toolOp.getGamepadButton(GamepadKeys.Button.A) whenActive {
            grip.position = 1.0
        }

        toolOp.getGamepadButton(GamepadKeys.Button.B) and !toolOp.getGamepadButton(GamepadKeys.Button.A) whenActive {
            grip.position = 0.0
        }

        waitForStart()

        while (opModeIsActive()) {
            CommandScheduler.getInstance().run()
        }
    }
}