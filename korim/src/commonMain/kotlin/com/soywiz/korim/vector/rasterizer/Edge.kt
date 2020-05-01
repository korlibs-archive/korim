package com.soywiz.korim.vector.rasterizer

import com.soywiz.korma.geom.*
import kotlin.math.*

internal data class Edge(val ax: Int, val ay: Int, val bx: Int, val by: Int, val wind: Int) {
    val minX = min(ax, bx)
    val maxX = max(ax, bx)
    val minY = min(ay, by)
    val maxY = max(ay, by)

    val isCoplanarX = ay == by
    val isCoplanarY = ax == bx
    val dy = (by - ay)
    val dx = (bx - ax)
    val slope = dy.toDouble() / dx.toDouble()
    var islope = 1.0 / slope

    val h = if (isCoplanarY) 0 else ay - (ax * dy) / dx

    fun containsY(y: Int): Boolean = y >= ay && y < by
    fun containsYNear(y: Int, offset: Int): Boolean = y >= (ay - offset) && y < (by + offset)
    fun intersectX(y: Int): Int = if (isCoplanarY) ax else ((y - h) * dx) / dy
    //fun intersectX(y: Double): Double = if (isCoplanarY) ax else ((y - h) * this.dx) / this.dy

    // Stroke extensions
    val angle = Angle.between(ax, ay, bx, by)
    val cos = angle.cosine
    val absCos = cos.absoluteValue
    val sin = angle.sine
    val absSin = sin.absoluteValue
}
