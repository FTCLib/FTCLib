package com.arcrobotics.ftclib.kotlin.extensions.geometry

import com.arcrobotics.ftclib.geometry.Rotation2d
import com.arcrobotics.ftclib.geometry.Translation2d

infix fun Translation2d.rotateBy(other: Rotation2d): Translation2d =
        this.rotateBy(other)
