package com.soywiz.korim.vector.rasterizer

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.doubleArrayListOf
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.toIntCeil
import com.soywiz.kmem.toIntFloor
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.interpolate
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

typealias RasterizerCallback = (x0: Double, x1: Double, y: Double) -> Unit

class Rasterizer {
    data class Edge(val a: IPoint, val b: IPoint, val wind: Int) {
        val minX = min(a.x, b.x)
        val maxX = max(a.x, b.x)
        val minY = min(a.y, b.y)
        val maxY = max(a.y, b.y)

        val isCoplanarX = a.y == b.y
        val isCoplanarY = a.x == b.x
        val slope = (b.y - a.y) / (b.x - a.x)
        val angle = Angle.between(a.x, a.y, b.x, b.y)
        val cos = angle.cosine
        //val cos = angle.sine
        val absCos = cos.absoluteValue
        val h = a.y - (a.x * slope)

        fun containsY(y: Double): Boolean = y >= a.y && y < b.y
        fun intersectX(y: Double): Double = if (isCoplanarY) a.x else ((y - h) / slope)
    }

    var debug: Boolean = false
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
    var quality: Int = 2

    fun rasterizeFill(bounds: Rectangle, quality: Int = this.quality, callback: RasterizerCallback) = rasterize(bounds, fill = true, callback = callback)

    fun rasterizeStroke(bounds: Rectangle, lineWidth: Double, quality: Int = this.quality, callback: RasterizerCallback) =
        run { this.strokeWidth = lineWidth }.also { rasterize(bounds, fill = false, callback = callback) }

    fun rasterize(bounds: Rectangle, fill: Boolean, callback: RasterizerCallback) {
        val xmin = bounds.left
        val xmax = bounds.right
        boundsBuilder.getBounds(tempRect)
        val startY = max(bounds.top, tempRect.top).toIntFloor()
        val endY = min(bounds.bottom, tempRect.bottom).toIntCeil()
        val func: (x0: Double, x1: Double, y: Double) -> Unit = { a, b, y ->
            //println("CHUNK")
            if (a <= xmax && b >= xmin) {
                //println("  - EMIT")
                val a0 = a.coerceIn(xmin, xmax)
                val b0 = b.coerceIn(xmin, xmax)
                if (debug) {
                    println("RASTER($a0, $b0, $y)")
                }
                callback(a0, b0, y)
            } else {
                // Discarded
                //println("  - DISCARDED")
            }
        }

        val yCount = (endY - startY + 1) * quality
        val yCountMax = kotlin.math.max((yCount - 1).toDouble(), 1.0)
        val step = 1.0 / quality.toDouble()

        yList.clear()
        for (n in 0 until yCount) {
            val ratio = n.toDouble() / yCount
            yList.add(ratio.interpolate(startY.toDouble(), endY.toDouble()))
        }

        if (fill) {
            internalRasterizeFill(yList, func)
        } else {
            internalRasterizeStroke(yList, func)
        }
    }
    private val yList = doubleArrayListOf()

    private fun internalRasterizeFill(
        yList: DoubleArrayList,
        callback: (x0: Double, x1: Double, y: Double) -> Unit
    ) {
        yList.fastForEach { y ->
            // @TODO: Optimize DoubleArrayList + inplace sort
            val xPoints = arrayListOf<Double>()
            iterateActiveEdgesAtY(y) {
                if (!it.isCoplanarX) {
                    xPoints.add(it.intersectX(y + 0.5))
                }
            }
            xPoints.sort()
            if (xPoints.size >= 2) {
                for (i in 0 until xPoints.size step 2) {
                    val a = xPoints[i]
                    val b = xPoints[i + 1]
                    callback(a, b, y)
                }
            }
        }
    }

    var strokeWidth: Double = 1.0

    private fun internalRasterizeStroke(
        yList: DoubleArrayList,
        callback: (x0: Double, x1: Double, y: Double) -> Unit
    ) {
        val strokeWidth2 = strokeWidth * 0.5
        yList.fastForEach { y ->
            iterateActiveEdgesAtY(y) {
                if (!it.isCoplanarX) {
                    val x = it.intersectX(y)
                    val hwidth = strokeWidth2 + strokeWidth2 * it.absCos
                    callback(x - hwidth, x + hwidth, y) // We should use slope to determine the actual width
                } else {
                    callback(it.minX, it.maxX, y)
                }
            }
        }
    }
}
