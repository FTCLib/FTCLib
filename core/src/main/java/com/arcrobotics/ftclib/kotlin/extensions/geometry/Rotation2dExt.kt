package com.arcrobotics.ftclib.kotlin.extensions.geometry

import com.arcrobotics.ftclib.geometry.Rotation2d

infix fun Rotation2d.rotateBy(other: Rotation2d): Rotation2d =
        this.rotateBy(other)
