package com.soywiz.korim.internal

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.absoluteValue
import kotlin.math.*

// @TODO: Move to KorMA

// y = (m * x) + b
// x = (y - b) / m
/*
internal data class Line(var m: Double, var b: Double) {
    // Aliases
    val slope get() = m
    val yIntercept get() = b
    val isXCoplanar get() = m == 0.0
    val isYCoplanar get() = m.isInfinite()

    companion object {
        fun fromTwoPoints(ax: Double, ay: Double, bx: Double, by: Double) = Line(0.0, 0.0).setFromTwoPoints(ax, ay, bx, by)
        fun getHalfLine(a: Line, b: Line, out: Line = Line(0.0, 0.0)): Line {


            return out.setFromTwoPoints()
        }
    }

    fun setFromTwoPoints(ax: Double, ay: Double, bx: Double, by: Double) = this.apply {
        this.m = (by - ay) / (bx - ax)
        // y = (slope * x) + b
        // ay = (slope * ax) + b
        // b = ay - (slope * ax)
        this.b = ay - (this.m * ax)
    }

    fun getY(x: Double) = if (isYCoplanar) 0.0 else (m * x) + b
    fun getX(y: Double) = if (isXCoplanar) 0.0 else (y - b) / m

    // y = (m0 * x) + b0
    // y = (m1 * x) + b1
    // (m0 * x) + b0 = (m1 * x) + b1
    // (m0 * x) = (m1 * x) + b1 - b0
    // (m0 * x) - (m1 * x) = b1 - b0
    // (m0 - m1) * x = b1 - b0
    // x = (b1 - b0) / (m0 - m1)
    fun getIntersectionX(other: Line): Double = (other.b - this.b) / (this.m - other.m)

    fun getIntersection(other: Line, out: Point = Point()): Point {
        val x = getIntersectionX(other)
        return out.setTo(x, getY(x))
    }

    fun getSegmentFromX(x0: Double, x1: Double) = LineSegment(x0, getY(x0), x1, getY(x1))
    fun getSegmentFromY(y0: Double, y1: Double) = LineSegment(getX(y0), y0, getX(y1), y1)
}

internal class LineSegment(ax: Double, ay: Double, bx: Double, by: Double) {
    var ax = ax; private set
    var ay = ay; private set
    var bx = bx; private set
    var by = by; private set
    val line = Line.fromTwoPoints(ax, ay, bx, by)
    fun setTo(ax: Double, ay: Double, bx: Double, by: Double) = this.apply {
        this.ax = ax
        this.ay = ay
        this.bx = bx
        this.by = by
        this.line.setFromTwoPoints(ax, ay, bx, by)
    }
    val slope get() = line.slope
    val length get() = Point.distance(ax, ay, bx, by)
}

internal data class Line(val ax: Double, val ay: Double, val bx: Double, val by: Double) {
    val minX get() = min(ax, bx)
    val maxX get() = max(ax, bx)
    val minY get() = min(ay, by)
    val maxY get() = max(ay, by)

    val isCoplanarX get() = ay == by
    val isCoplanarY get() = ax == bx
    val dy get() = (by - ay)
    val dx get() = (bx - ax)
    val slope get() = dy / dx
    val islope get() = 1.0 / slope

    val h = if (isCoplanarY) 0.0 else ay - (ax * dy) / dx

    fun containsY(y: Double): Boolean = y >= ay && y < by
    fun containsYNear(y: Double, offset: Double): Boolean = y >= (ay - offset) && y < (by + offset)
    fun getX(y: Double): Double = if (isCoplanarY) ax else ((y - h) * dx) / dy
    fun getY(x: Double): Double = if (isCoplanarX) ay else TODO()
    fun intersect(line: Line, out: Point = Point()): Point? {

    }
    //fun intersectX(y: Double): Double = if (isCoplanarY) ax else ((y - h) * this.dx) / this.dy

    // Stroke extensions
    val angle = Angle.between(ax, ay, bx, by)
    val cos = angle.cosine
    val absCos = cos.absoluteValue
    val sin = angle.sine
    val absSin = sin.absoluteValue
}
*/
