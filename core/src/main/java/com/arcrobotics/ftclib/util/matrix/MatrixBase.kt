package com.arcrobotics.ftclib.util.matrix

import com.arcrobotics.ftclib.util.matrix.util.Size
import com.arcrobotics.ftclib.util.matrix.util.Slice
import java.util.stream.Stream

interface MatrixBase<T>: Iterable<T> where T: Any {
    var size: Size

    val rowLength: Int

    val rowRange: IntRange
        get() = 0 until rowLength

    val colLength: Int

    val colRange: IntRange
        get() = 0 until colLength

    fun transpose(): MatrixBase<T>

    fun stream(): Stream<T>

    override operator fun iterator(): Iterator<T>

    val withIndices: Iterator<Triple<T, Int, Int>>

    infix fun equal(other: MatrixBase<T>): MatrixBase<Boolean>

    operator fun get(index: Int): List<T>

    operator fun get(indexSlice: Slice): MatrixBase<T>

    operator fun get(indexR: Int, indexC: Int): T

    operator fun get(indexRSlice: Slice, indexCSlice: Slice): MatrixBase<T>

    operator fun get(indexRSlice: Slice, indexC: Int): List<T>

    operator fun get(indexR: Int, indexCSlice: Slice): MatrixBase<T>

    operator fun set(index: Int, value: List<T>)

    operator fun set(indexSlice: Slice, value: MatrixBase<T>)

    operator fun set(indexR: Int, indexC: Int, value: T)

    operator fun set(indexRSlice: Slice, indexCSlice: Slice, value: MatrixBase<T>)

    operator fun set(indexRSlice: Slice, indexC: Int, value: List<T>)

    operator fun set(indexR: Int, indexCSlice: Slice, value: List<T>)

    fun rowAppend(other: MatrixBase<T>): MatrixBase<T>

    fun removeRow(indexR: Int): List<T>

    fun colAppend(other: MatrixBase<T>): MatrixBase<T>

    fun removeCol(indexC: Int): List<T>

    fun toList(): List<List<T>>
}