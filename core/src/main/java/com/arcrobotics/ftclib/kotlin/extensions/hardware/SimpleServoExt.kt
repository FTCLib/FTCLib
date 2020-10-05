package com.arcrobotics.ftclib.kotlin.extensions.hardware

import com.arcrobotics.ftclib.hardware.SimpleServo

private typealias Range = Pair<Number, Number>

var SimpleServo.range: Range
    get() = 0 to this.angleRange
    set(value) {
        this.setRange(value.first.toDouble(), value.second.toDouble())
    }