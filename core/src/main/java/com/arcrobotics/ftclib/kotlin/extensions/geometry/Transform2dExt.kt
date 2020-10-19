package com.arcrobotics.ftclib.kotlin.extensions.geometry

import com.arcrobotics.ftclib.geometry.Transform2d

/**
 * @author Jaran Chao
 *
 * Add quality of life update to inverse() for it to be a property of the Transform2d class and not
 * a function. Function notation is still callable. Goal is to mimic Kotlin getter call
 * (since the function was not prefaced with get, like getInverse(), Kotlin didn't pick up on the
 * fact that it is a "getter")
 */
val Transform2d.inverse: Transform2d
    get() = this.inverse()
