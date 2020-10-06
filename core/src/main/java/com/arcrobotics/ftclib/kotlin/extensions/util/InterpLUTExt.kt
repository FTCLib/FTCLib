package com.arcrobotics.ftclib.kotlin.extensions.util

import com.arcrobotics.ftclib.util.InterpLUT

operator fun InterpLUT.get(input: Number): Double =
        this[input.toDouble()]

operator fun InterpLUT.set(input: Number, output: Number) {
    this.add(input.toDouble(), output.toDouble())
}
