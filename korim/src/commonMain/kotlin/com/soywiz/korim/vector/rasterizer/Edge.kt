package com.soywiz.korim.vector.rasterizer

import com.soywiz.korim.internal.*
import com.soywiz.korim.internal.floorCeil
import com.soywiz.korma.geom.*
import kotlin.math.*

@Suppress("DuplicatedCode")
internal class Edge {
    fun getX(n: Int) = if (n == 0) this.ax else this.bx
    fun getY(n: Int) = if (n == 0) this.ay else this.by

    companion object {
        operator fun invoke(ax: Int, ay: Int, bx: Int, by: Int, wind: Int = 0) = Edge().setTo(ax, ay, bx, by, wind)

        fun getIntersectY(a: Edge, b: Edge): Int {
            getIntersectXY(a, b) { x, y -> return y.toInt() }
            return Int.MIN_VALUE
        }

        fun getIntersectX(a: Edge, b: Edge): Int {
            getIntersectXY(a, b) { x, y -> return x.toInt() }
            return Int.MIN_VALUE
        }

        fun areParallel(a: Edge, b: Edge) = ((a.by - a.ay) * (b.ax - b.bx)) - ((b.by - b.ay) * (a.ax - a.bx)) == 0

        fun getIntersectXY(a: Edge, b: Edge, out: Point = Point()): Point? {
            getIntersectXY(a, b) { x, y -> return out.setTo(x, y) }
            return null
        }

        fun getIntersectXYInt(a: Edge, b: Edge, out: PointInt = PointInt()): PointInt? {
            getIntersectXY(a, b) { x, y -> return out.setTo(x.toInt(), y.toInt()) }
            return null
        }

        fun angleBetween(a: Edge, b: Edge): Angle {
            return b.angle - a.angle
        }

        // https://www.geeksforgeeks.org/program-for-point-of-intersection-of-two-lines/
        inline fun getIntersectXY(a: Edge, b: Edge, out: (x: Double, y: Double) -> Unit): Boolean {
            val Ax: Double = a.ax.toDouble()
            val Ay: Double = a.ay.toDouble()
            val Bx: Double = a.bx.toDouble()
            val By: Double = a.by.toDouble()
            val Cx: Double = b.ax.toDouble()
            val Cy: Double = b.ay.toDouble()
            val Dx: Double = b.bx.toDouble()
            val Dy: Double = b.by.toDouble()
            val a1 = By - Ay
            val b1 = Ax - Bx
            val c1 = a1 * (Ax) + b1 * (Ay)
            val a2 = Dy - Cy
            val b2 = Cx - Dx
            val c2 = a2 * (Cx) + b2 * (Cy)
            val determinant = a1 * b2 - a2 * b1
            if (determinant == 0.0) return false
            val x = (b2 * c1 - b1 * c2) / determinant
            val y = (a1 * c2 - a2 * c1) / determinant
            out(floorCeil(x), floorCeil(y))
            return true
        }

        inline fun getIntersectXY(Ax: Double, Ay: Double, Bx: Double, By: Double, Cx: Double, Cy: Double, Dx: Double, Dy: Double, out: (x: Double, y: Double) -> Unit): Boolean {
            val a1 = By - Ay
            val b1 = Ax - Bx
            val c1 = a1 * (Ax) + b1 * (Ay)
            val a2 = Dy - Cy
            val b2 = Cx - Dx
            val c2 = a2 * (Cx) + b2 * (Cy)
            val determinant = a1 * b2 - a2 * b1
            if (determinant == 0.0) return false
            val x = (b2 * c1 - b1 * c2) / determinant
            val y = (a1 * c2 - a2 * c1) / determinant
            out(x, y)
            return true
        }
    }

    var ax = 0; private set
    var ay = 0; private set
    var bx = 0; private set
    var by = 0; private set
    var wind: Int = 0; private set

    var dy: Int = 0; private set
    var dx: Int = 0; private set
    var isCoplanarX: Boolean = false; private set
    var isCoplanarY: Boolean = false; private set

    var h: Int = 0; private set

    fun copyFrom(other: Edge) = setTo(other.ax, other.ay, other.bx, other.by, other.wind)

    fun setTo(ax: Int, ay: Int, bx: Int, by: Int, wind: Int) = this.apply {
        this.ax = ax
        this.ay = ay
        this.bx = bx
        this.by = by
        this.dx = bx - ax
        this.dy = by - ay
        this.isCoplanarX = ay == by
        this.isCoplanarY = ax == bx
        this.wind = wind
        this.h = if (isCoplanarY) 0 else ay - (ax * dy) / dx
    }

    fun setToHalf(a: Edge, b: Edge): Edge = this.apply {
        val minY = min(a.minY, b.minY)
        val maxY = min(a.maxY, b.maxY)
        val minX = (a.intersectX(minY) + b.intersectX(minY)) / 2
        val maxX = (a.intersectX(maxY) + b.intersectX(maxY)) / 2
        setTo(minX, minY, maxX, maxY, +1)
    }

    val minX get() = min(ax, bx)
    val maxX get() = max(ax, bx)
    val minY get() = min(ay, by)
    val maxY get() = max(ay, by)

    fun containsY(y: Int): Boolean = y >= ay && y < by
    fun containsYNear(y: Int, offset: Int): Boolean = y >= (ay - offset) && y < (by + offset)
    fun intersectX(y: Int): Int = if (isCoplanarY) ax else ((y - h) * dx) / dy
    //fun intersectX(y: Double): Double = if (isCoplanarY) ax else ((y - h) * this.dx) / this.dy

    // Stroke extensions
    val angle get() = Angle.between(ax, ay, bx, by)
    val cos get() = angle.cosine
    val absCos get() = cos.absoluteValue
    val sin get() = angle.sine
    val absSin get() = sin.absoluteValue

    override fun toString(): String = "Edge([$ax,$ay]-[$bx,$by])"
}
