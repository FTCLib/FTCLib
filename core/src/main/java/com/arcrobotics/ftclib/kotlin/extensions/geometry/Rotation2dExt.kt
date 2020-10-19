package com.arcrobotics.ftclib.kotlin.extensions.geometry

import com.arcrobotics.ftclib.geometry.Rotation2d

/**
 * @author Jaran Chao
 *
 * Add quality of life update to be able to call rotateBy in infix notation
 */
infix fun Rotation2d.rotateBy(other: Rotation2d): Rotation2d =
        this.rotateBy(other)
