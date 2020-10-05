package com.arcrobotics.ftclib.kotlin.extensions.util

import com.arcrobotics.ftclib.util.LUT

fun <T, R> Map<T, R>.toLUT(): LUT<T, R> where T: Number {
    val ret = LUT<T, R>()
    for (i in this) {
        ret[i.key] = i.value
    }
    return ret
}
