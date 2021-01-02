package com.arcrobotics.ftclib.kotlin.extensions.hardware

import com.arcrobotics.ftclib.hardware.SimpleServo

/**
 * @author Jaran Chao
 *
 * Providing a quality of life update on getter and setter for the range of a simpleServo
 * as a Pair of Numbers to represent the range. Allowing for idiomatic Kotlin getter and setter
 * calls on the range of a SimpleServo
 */
private typealias Range = Pair<Number, Number> // type alias only in file for clarity

private var offset: Pair<Double, Double> = 0.0 to 0.0 // keep track of angle offset

var SimpleServo.range: Range
    get() = (0 - offset.first) to (this.angleRange - offset.second)
    set(value) {
        offset = (0 - value.first.toDouble()) to (0 - value.second.toDouble())
        this.setRange(value.first.toDouble(), value.second.toDouble())
    }