package com.arcrobotics.ftclib.kotlin.extensions.geometry

import com.arcrobotics.ftclib.geometry.Transform2d

val Transform2d.inverse: Transform2d
    get() = this.inverse()
