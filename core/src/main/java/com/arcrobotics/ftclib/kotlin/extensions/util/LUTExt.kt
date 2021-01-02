package com.arcrobotics.ftclib.kotlin.extensions.util

import com.arcrobotics.ftclib.util.LUT

/**
 * @author Jaran Chao
 *
 * Even though LUT extends HashMap, there is no function to treat a Map<T: Number, R> as a
 * LUT<T: Number, R>. Extension function allows for easy conversion from Map<T: Number, R> to a
 * LUT<T: Number, R>.
 */
fun <T, R> Map<T, R>.toLUT(): LUT<T, R> where T : Number {
    val ret = LUT<T, R>()
    for (i in this) {
        ret[i.key] = i.value
    }
    return ret
}
