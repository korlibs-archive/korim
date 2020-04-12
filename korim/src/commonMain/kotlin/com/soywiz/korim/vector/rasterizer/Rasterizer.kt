package com.soywiz.korim.vector.rasterizer

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.doubleArrayListOf
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.toIntCeil
import com.soywiz.kmem.toIntFloor
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.interpolate
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

typealias RasterizerCallback = (x0: Double, x1: Double, y: Double) -> Unit

class Rasterizer {
    companion object {
        const val FIXED_SCALE = 32
    }

    data class Edge(val ax: Double, val ay: Double, val bx: Double, val by: Double, val wind: Int) {
        val minX = min(ax, bx)
        val maxX = max(ax, bx)
        val minY = min(ay, by)
        val maxY = max(ay, by)

        val isCoplanarX = ay == by
        val isCoplanarY = ax == bx
        val slope = (by - ay) / (bx - ax)

        val angle = Angle.between(ax, ay, bx, by)
        val cos = angle.cosine
        val absCos = cos.absoluteValue

        val h = ay - (ax * slope)

        fun containsY(y: Double): Boolean = y >= ay && y < by
        fun intersectX(y: Double): Double = if (isCoplanarY) ax else ((y - h) / slope)
    }

    var debug: Boolean = false
    private val tempRect = Rectangle()
    private val boundsBuilder = BoundsBuilder()
    private val pointsX = doubleArrayListOf()
    private val pointsY = doubleArrayListOf()

    @PublishedApi
    internal val edges = arrayListOf<Edge>()

    fun getBounds(out: Rectangle = Rectangle()) = boundsBuilder.getBounds(out)

    fun reset() {
        boundsBuilder.reset()
        pointsX.clear()
        pointsY.clear()
    }

    private fun addEdge(ax: Double, ay: Double, bx: Double, by: Double) {
        edges.add(if (ay < by) Edge(ax, ay, bx, by, +1) else Edge(bx, by, ax, ay, -1))
    }

    private fun addEdge(a: Int, b: Int) {
        addEdge(pointsX[a], pointsY[a], pointsX[b], pointsY[b])
    }

    val size get() = pointsX.size
    fun add(x: Double, y: Double) {
        pointsX.add(x)
        pointsY.add(y)
        boundsBuilder.add(x, y)
        if (size >= 2) addEdge(size - 2, size - 1)
    }

    inline fun add(x: Number, y: Number) = add(x.toDouble(), y.toDouble())

    inline fun forEachActiveEdgeAtY(y: Double, block: (Edge) -> Unit) {
        // @TODO: Optimize this. We can sort edges by Y and perform a binary search?
        edges.fastForEach { edge ->
            if (edge.containsY(y)) {
                block(edge)
            }
        }
    }

    fun close() {
        if (size >= 2) {
            addEdge(size - 1, 0)
        }
    }
    var quality: Int = 2

    fun rasterizeFill(bounds: Rectangle, quality: Int = this.quality, stats: RasterizeStats? = null, callback: RasterizerCallback) =
        rasterize(bounds, true, quality, stats, callback)

    fun rasterizeStroke(bounds: Rectangle, lineWidth: Double, quality: Int = this.quality, stats: RasterizeStats? = null, callback: RasterizerCallback) =
        run { this.strokeWidth = lineWidth }.also { rasterize(bounds, false, quality, stats, callback) }

    class RasterizeStats {
        var iterationsCount: Int = 0
        fun reset() {
            iterationsCount = 0
        }
        fun addIterations(count: Int) {
            iterationsCount += count
        }
    }

    fun rasterize(bounds: Rectangle, fill: Boolean, quality: Int = this.quality, stats: RasterizeStats? = null, callback: RasterizerCallback) {
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
            internalRasterizeFill(yList, stats, func)
        } else {
            internalRasterizeStroke(yList, stats, func)
        }
    }
    private val yList = doubleArrayListOf()

    private fun internalRasterizeFill(
        yList: DoubleArrayList,
        stats: RasterizeStats?,
        callback: (x0: Double, x1: Double, y: Double) -> Unit
    ) {
        var iterationsCount = 0
        yList.fastForEach { y ->
            // @TODO: Optimize DoubleArrayList + inplace sort
            val xPoints = arrayListOf<Double>()
            forEachActiveEdgeAtY(y) {
                iterationsCount++
                if (!it.isCoplanarX) {
                    xPoints.add(it.intersectX(y + 0.5))
                }
            }
            xPoints.sort()
            if (xPoints.size >= 2) {
                for (i in 0 until xPoints.size - 1 step 2) {
                    iterationsCount++
                    val a = xPoints[i]
                    val b = xPoints[i + 1]
                    callback(a, b, y)
                }
            }
        }
        stats?.addIterations(iterationsCount)
    }

    var strokeWidth: Double = 1.0

    private fun internalRasterizeStroke(
        yList: DoubleArrayList,
        stats: RasterizeStats?,
        callback: (x0: Double, x1: Double, y: Double) -> Unit
    ) {
        var iterationsCount = 0
        val strokeWidth2 = strokeWidth * 0.5
        yList.fastForEach { y ->
            forEachActiveEdgeAtY(y) {
                iterationsCount++
                if (!it.isCoplanarX) {
                    val x = it.intersectX(y)
                    val hwidth = strokeWidth2 + strokeWidth2 * it.absCos
                    callback(x - hwidth, x + hwidth, y) // We should use slope to determine the actual width
                } else {
                    callback(it.minX, it.maxX, y)
                }
            }
        }
        stats?.addIterations(iterationsCount)
    }
}
