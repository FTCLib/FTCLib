package com.arcrobotics.ftclib.kotlin.extensions.drivebase

import com.arcrobotics.ftclib.drivebase.RobotDrive

/**
 * @author Jaran Chao
 *
 * Adding quality of life updates to setRange, setMaxSpeed, and clipRange to be able to be called in
 * infix notation. Also to take in parameters as Numbers to eliminate the need to constantly
 * do .toDouble() on inputs that are not Doubles
 */
private typealias Range = Pair<Number, Number> // type alias only in file for clarity

infix fun <T> T.setRange(range: Range) where T : RobotDrive =
        this.setRange(range.first.toDouble(), range.second.toDouble())

infix fun <T> T.setMaxSpeed(value: Number) where T : RobotDrive =
        this.setMaxSpeed(value.toDouble())

infix fun <T> T.clipRange(value: Number): Double where T : RobotDrive =
        this.clipRange(value.toDouble())
