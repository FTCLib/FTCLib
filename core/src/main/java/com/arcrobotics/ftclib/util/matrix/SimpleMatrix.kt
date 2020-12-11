package com.arcrobotics.ftclib.util.matrix

import com.arcrobotics.ftclib.util.matrix.util.Size
import com.arcrobotics.ftclib.util.matrix.util.by

class SimpleMatrix(size: Size, initBlock: (Int, Int) -> Double = {_, _ -> 0.0}): Matrix<Double>(size, initBlock) {
    constructor(mat: Matrix<out Number>): this(mat.size, {r, c -> mat[r, c].toDouble()})
    override val t: SimpleMatrix
        get() = SimpleMatrix(super.transpose())

    operator fun plus(other: SimpleMatrix): SimpleMatrix {
        val ret = SimpleMatrix(this.size)
        for ((v, r, c) in this.withIndices) {
            ret[r, c] = v + other[r, c]
        }
        return ret
    }

    operator fun minus(other: SimpleMatrix): SimpleMatrix {
        val ret = SimpleMatrix(this.size)
        for ((v, r, c) in this.withIndices) {
            ret[r, c] = v - other[r, c]
        }
        return ret
    }

    operator fun times(other: SimpleMatrix): SimpleMatrix {
        val ret = SimpleMatrix(this.size)
        for ((v, r, c) in this.withIndices) {
            ret[r, c] = v * other[r, c]
        }
        return ret
    }

    operator fun div(other: SimpleMatrix): SimpleMatrix {
        val ret = SimpleMatrix(this.size)
        for ((v, r, c) in this.withIndices) {
            ret[r, c] = v / other[r, c]
        }
        return ret
    }

    fun matMult(other: SimpleMatrix): SimpleMatrix {
        if (this.size.y != other.size.x) throw IllegalArgumentException("${this.size} is not compatible with ${other.size}")
        val ret = SimpleMatrix(this.size.x by other.size.y)
        val other_t = other.t
        for (i in rowRange) {
            for (j in colRange) {
                ret[i, j] = this[i].mapIndexed { index, d -> d * other_t[j][index] }.sum()
            }
        }
        return ret
    }

    // TODO Jaran find effective PINV algo (QR?)
}