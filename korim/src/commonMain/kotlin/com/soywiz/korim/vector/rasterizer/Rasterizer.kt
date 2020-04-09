package com.soywiz.korim.vector.rasterizer

import com.soywiz.kmem.toIntCeil
import com.soywiz.kmem.toIntFloor
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Rectangle
import kotlin.math.max
import kotlin.math.min

// @TODO: We should optimize this
typealias RasterizerFill = (a: Double, b: Double, y: Int, alpha: Double) -> Unit

class Rasterizer {
    interface PaintSegment {
        fun paint(a: Double, b: Double, y: Int, alpha: Double)

        companion object {
            operator fun invoke(callback: RasterizerFill) = object : PaintSegment {
                override fun paint(a: Double, b: Double, y: Int, alpha: Double) = callback(a, b, y, alpha)
            }
        }
    }

    data class Edge(val a: IPoint, val b: IPoint, val wind: Int) {
        val minX = min(a.x, b.x)
        val maxX = max(a.x, b.x)
        val minY = min(a.y, b.y)
        val maxY = max(a.y, b.y)

        val isCoplanarX = a.y == b.y
        val isCoplanarY = a.x == b.x
        val slope = (b.y - a.y) / (b.x - a.x)
        val h = a.y - (a.x * slope)

        fun containsY(y: Double): Boolean = y >= a.y && y < b.y
        fun intersectX(y: Double): Double = if (isCoplanarY) a.x else ((y - h) / slope)
    }

    private val tempRect = Rectangle()
    private val boundsBuilder = BoundsBuilder()
    private val points = arrayListOf<IPoint>()

    @PublishedApi
    internal val edges = arrayListOf<Edge>()

    fun getBounds(out: Rectangle = Rectangle()) = boundsBuilder.getBounds(out)

    fun reset() {
        boundsBuilder.reset()
        points.clear()
    }

    private fun addEdge(a: IPoint, b: IPoint) {
        edges.add(if (a.y < b.y) Edge(a, b, +1) else Edge(b, a, -1))
    }

    fun add(x: Double, y: Double) {
        val p = IPoint(x, y)
        points.add(p)
        boundsBuilder.add(x, y)
        if (points.size >= 2) {
            addEdge(points[points.size - 2], points[points.size - 1])
        }
    }

    inline fun add(x: Number, y: Number) = add(x.toDouble(), y.toDouble())

    inline fun iterateActiveEdgesAtY(y: Double, block: (Edge) -> Unit) {
        // @TODO: Optimize this. We can sort edges by Y and perform a binary search?
        for (edge in edges) {
            if (edge.containsY(y)) {
                block(edge)
            }
        }
    }

    fun close() {
        if (points.size >= 2) {
            addEdge(points[points.size - 1], points[0])
        }
    }

    fun rasterizeFill(bounds: Rectangle, callback: PaintSegment) = _rasterize(bounds, fill = true, callback = callback)

    fun rasterizeStroke(bounds: Rectangle, lineWidth: Double, callback: PaintSegment) =
        run { this.strokeWidth = lineWidth }.also { _rasterize(bounds, fill = false, callback = callback) }

    fun rasterizeFill(bounds: Rectangle, callback: RasterizerFill) = rasterizeFill(bounds, PaintSegment(callback))
    fun rasterizeStroke(bounds: Rectangle, lineWidth: Double, callback: RasterizerFill) =
        rasterizeStroke(bounds, lineWidth, PaintSegment(callback))

    private fun _rasterize(bounds: Rectangle, fill: Boolean, callback: PaintSegment) {
        val xmin = bounds.left
        val xmax = bounds.right
        boundsBuilder.getBounds(tempRect)
        val startY = max(bounds.top, tempRect.top).toIntFloor()
        val endY = min(bounds.bottom, tempRect.bottom).toIntCeil()
        val func: (x0: Double, x1: Double, y: Int, alpha: Double) -> Unit = { a, b, y, alpha ->
            //println("CHUNK")
            if (a <= xmax && b >= xmin) {
                //println("  - EMIT")
                val a0 = a.coerceIn(xmin, xmax)
                val b0 = b.coerceIn(xmin, xmax)
                callback.paint(a0, b0, y, alpha)
            } else {
                // Discarded
                //println("  - DISCARDED")
            }
        }
        if (fill) {
            internalRasterizeFill(startY, endY, func)
        } else {
            internalRasterizeStroke(startY, endY, func)
        }
    }

    private fun internalRasterizeFill(
        startY: Int,
        endY: Int,
        callback: (x0: Double, x1: Double, y: Int, alpha: Double) -> Unit
    ) {
        for (n in startY..endY) {
            // @TODO: Optimize DoubleArrayList + inplace sort
            val y = n.toDouble()
            val xPoints = arrayListOf<Double>()
            iterateActiveEdgesAtY(y) {
                if (!it.isCoplanarX) {
                    xPoints.add(it.intersectX(y))
                }
            }
            xPoints.sort()
            if (xPoints.size >= 2) {
                for (i in 0 until xPoints.size step 2) {
                    val a = xPoints[i]
                    val b = xPoints[i + 1]
                    callback(a, b, n, 1.0)
                }
            }
        }
    }

    private var strokeWidth: Double = 1.0

    private fun internalRasterizeStroke(
        startY: Int,
        endY: Int,
        callback: (x0: Double, x1: Double, y: Int, alpha: Double) -> Unit
    ) {
        val strokeWidth2 = strokeWidth * 0.5
        for (n in startY..endY) {
            // @TODO: Optimize DoubleArrayList + inplace sort
            val y = n.toDouble()
            iterateActiveEdgesAtY(y) {
                if (!it.isCoplanarX) {
                    val x = it.intersectX(y)
                    val hwidth = strokeWidth2
                    callback(x - hwidth, x + hwidth, n, 1.0) // We should use slope to determine the actual width
                } else {
                    callback(it.minX, it.maxX, n, 1.0)
                }
            }
        }
    }
}
