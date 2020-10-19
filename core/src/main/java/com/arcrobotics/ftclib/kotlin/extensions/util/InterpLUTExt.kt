package com.arcrobotics.ftclib.kotlin.extensions.util

import com.arcrobotics.ftclib.util.InterpLUT

/**
 * @author Jaran Chao
 *
 * Providing get and set operators on InterpLUT.
 *
 * Kotlin detected a get operator, but only takes parameter (input) as a Double. Updated new get
 * operator to parameter (input) as Number to mimic Java implicit widening and getting rid of
 * unneeded .toDouble() calls
 *
 * Kotlin did not detect set operator as the equivalent was called add. Creating set operator and
 * takes parameters (input, output) as Number to remove unneeded .toDouble() calls
 */
operator fun InterpLUT.get(input: Number): Double =
        this[input.toDouble()]

operator fun InterpLUT.set(input: Number, output: Number) {
    this.add(input.toDouble(), output.toDouble())
}
