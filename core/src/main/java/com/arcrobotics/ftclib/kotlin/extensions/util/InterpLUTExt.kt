package com.arcrobotics.ftclib.kotlin.extensions.util

import com.arcrobotics.ftclib.util.InterpLUT

operator fun InterpLUT.get(`in`: Number): Double =
        this[`in`.toDouble()]

operator fun InterpLUT.set(`in`: Number, out: Number) {
    this.add(`in`.toDouble(), out.toDouble())
}
