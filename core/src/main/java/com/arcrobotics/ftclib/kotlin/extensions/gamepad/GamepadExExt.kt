package com.arcrobotics.ftclib.kotlin.extensions.gamepad

import com.arcrobotics.ftclib.command.InstantCommand
import com.arcrobotics.ftclib.command.Subsystem
import com.arcrobotics.ftclib.command.button.Trigger

operator fun <T: Trigger> T.not(): Trigger = this.negate()

infix fun <T: Trigger> T.and(other: T): Trigger = this.and(other)

infix fun <T: Trigger> T.whenActive(command: () -> Unit): Trigger =
        this.whenActive(InstantCommand(command))

fun <T: Trigger> T.whenActive(vararg requirements: Subsystem, command: () -> Unit) =
        this.whenActive(InstantCommand(command, *requirements))

// TODO @JarnaChao DSL maybe