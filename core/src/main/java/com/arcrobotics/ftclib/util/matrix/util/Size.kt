package com.arcrobotics.ftclib.util.matrix.util

data class Size(val x: Int, val y: Int) {
    val reversed
        get() = this.y by this.x

    operator fun plus(other: Size): Size =
            Size(this.x + other.x, this.y + other.y)

    operator fun minus(other: Size): Size =
            Size(this.x - other.x, this.y - other.y)

    operator fun times(other: Int): Size =
            Size(this.x * other, this.y * other)

    fun scale(other: Int) = this * other

    companion object {
        @JvmStatic
        fun sizeOf(x: Int, y: Int) = x by y

        @JvmStatic
        @JvmName("add")
        fun sps(src: Size, other: Size) = src + other

        @JvmStatic
        @JvmName("subtract")
        fun sms(src: Size, other: Size) = src - other
    }
}

infix operator fun Int.times(other: Size): Size =
        Size(this * other.x, this * other.y)

infix fun Int.by(other: Int): Size = Size(this, other)