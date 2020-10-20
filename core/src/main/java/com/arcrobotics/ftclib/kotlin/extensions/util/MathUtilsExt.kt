package com.arcrobotics.ftclib.kotlin.extensions.util

import com.arcrobotics.ftclib.util.MathUtils

/**
 * @author Jaran Chao
 *
 * Adding quality of life updates to MathUtils.clamp function to now be able to be called as
 * extension functions of type Int and Double respectively. Inputs are generalized to Number then
 * casted to which the type it was called.
 *
 * Example:
 * 10.0.clamp(1,11) // 1 and 11 are converted to Doubles to satisfy type widening
 *
 * 10.clamp(1.5, 11.5) // 10 is converted to Double to satisfy type widening
 *
 * 10.clamp(1, 11.5) // 1 is inputted as Number, 11.5 is inputted as Double, therefore 1 and 10 are
 * casted to Double to satisfy type widening
 */
fun Int.clamp(low: Number, high: Number): Int =
        MathUtils.clamp(this, low.toInt(), high.toInt())

fun Int.clamp(low: Double, high: Number): Double =
        MathUtils.clamp(this.toDouble(), low, high.toDouble())

fun Int.clamp(low: Number, high: Double): Double =
        MathUtils.clamp(this.toDouble(), low.toDouble(), high)

fun Int.clamp(low: Double, high: Double): Double =
        MathUtils.clamp(this.toDouble(), low, high)

fun Double.clamp(low: Number, high: Number): Double =
        MathUtils.clamp(this, low.toDouble(), high.toDouble())
