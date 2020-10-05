package com.arcrobotics.ftclib.kotlin.extensions.geometry

import com.arcrobotics.ftclib.geometry.Vector2d

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
