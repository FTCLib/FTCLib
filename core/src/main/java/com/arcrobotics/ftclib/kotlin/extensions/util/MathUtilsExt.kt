package com.arcrobotics.ftclib.kotlin.extensions.util

import com.arcrobotics.ftclib.util.MathUtils

fun Int.clamp(low: Number, high: Number): Int =
        MathUtils.clamp(this, low.toInt(), high.toInt())

fun Double.clamp(low: Number, high: Number): Double =
        MathUtils.clamp(this, low.toDouble(), high.toDouble())
