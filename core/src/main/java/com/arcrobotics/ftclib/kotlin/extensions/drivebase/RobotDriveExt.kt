package com.arcrobotics.ftclib.kotlin.extensions.drivebase

import com.arcrobotics.ftclib.drivebase.RobotDrive

private typealias Range = Pair<Number, Number>

infix fun <T> T.setRange(range: Range) where T: RobotDrive =
        this.setRange(range.first.toDouble(), range.second.toDouble())

infix fun <T> T.setMaxSpeed(value: Number) where T: RobotDrive =
        this.setMaxSpeed(value.toDouble())

infix fun <T> T.clipRange(value: Number): Double where T: RobotDrive =
        this.clipRange(value.toDouble())
