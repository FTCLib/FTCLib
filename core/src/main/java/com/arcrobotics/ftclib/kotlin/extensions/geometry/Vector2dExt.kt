package com.arcrobotics.ftclib.kotlin.extensions.geometry

import com.arcrobotics.ftclib.geometry.Vector2d

/**
 * @author Jaran Chao
 *
 * Adding quality of life updates for angle, magnitude, rotateBy, dot, scale, and scalarProject
 *
 * angle, magnitude: properties of the Vector2d class instead of functions. Function is still
 * callable. Gaol is to mimic Kotlin getter call (since the functions were not prefaced with get,
 * like getAngle() and getMagnitude(), Kotlin didn't pick up on the fact that they are "getters")
 *
 * rotateBy, dot, scale, scalarProject: now able to call in infix notation, and also updated
 * parameters to be Number instead of Double to mimic Java implicit widening and eliminating the
 * fact that .toDouble() calls are required on inputs that aren't of type Double
 */
val Vector2d.angle
    get() = this.angle()

val Vector2d.magnitude
    get() = this.magnitude()

infix fun Vector2d.rotateBy(angle: Number): Vector2d =
        this.rotateBy(angle.toDouble())

infix fun Vector2d.dot(other: Vector2d) =
        this.dot(other)

infix fun Vector2d.scale(other: Number): Vector2d =
        this.scale(other.toDouble())

infix fun Vector2d.scalarProject(other: Vector2d) =
        this.scalarProject(other)
