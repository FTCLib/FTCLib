package com.arcrobotics.ftclib.kotlin.extensions.gamepad

import com.arcrobotics.ftclib.command.InstantCommand
import com.arcrobotics.ftclib.command.button.Trigger

operator fun <T : Trigger> T.not(): Trigger = this.negate()

infix fun <T : Trigger> T.and(other: T): Trigger = this.and(other)

infix fun <T : Trigger> T.whenActive(command: () -> Unit): Trigger =
        this.whenActive(InstantCommand(command))

infix fun <T : Trigger> T.whileActiveContinuous(command: () -> Unit): Trigger =
        this.whileActiveContinuous(InstantCommand(command))

infix fun <T : Trigger> T.whileActiveOnce(command: () -> Unit): Trigger =
        this.whileActiveOnce(InstantCommand(command))

infix fun <T : Trigger> T.whenInactive(command: () -> Unit): Trigger =
        this.whenInactive(InstantCommand(command))

infix fun <T : Trigger> T.toggleWhenActive(command: () -> Unit): Trigger =
        this.toggleWhenActive(InstantCommand(command))

infix fun <T : Trigger> T.toggleWhenActive(commands: Pair<() -> Unit, () -> Unit>): Trigger =
        this.toggleWhenActive(InstantCommand(commands.first), InstantCommand(commands.second))

infix fun <T : Trigger> T.cancelWhenActive(command: () -> Unit): Trigger =
        this.cancelWhenActive(InstantCommand(command))