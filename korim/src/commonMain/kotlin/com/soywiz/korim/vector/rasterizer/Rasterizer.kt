package com.soywiz.korim.vector.rasterizer

import com.soywiz.kds.*
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korma.geom.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

typealias RasterizerCallback = (x0: Int, x1: Int, y: Int) -> Unit

const val RAST_FIXED_SCALE = 32 // Important NOTE: Power of two so divisions are >> and remaining &
const val RAST_FIXED_SCALE_HALF = (RAST_FIXED_SCALE / 2) - 1
//const val RAST_FIXED_SCALE_HALF = (RAST_FIXED_SCALE / 2)
//const val RAST_FIXED_SCALE_HALF = 0

class Rasterizer {
    data class Edge(val ax: Int, val ay: Int, val bx: Int, val by: Int, val wind: Int) {
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

    var debug: Boolean = false
    private val tempRect = Rectangle()
    private val boundsBuilder = BoundsBuilder()
    private val pointsX = DoubleArrayList(1024)
    private val pointsY = DoubleArrayList(1024)

    @PublishedApi
    internal val edges = arrayListOf<Edge>()

    fun getBounds(out: Rectangle = Rectangle()) = boundsBuilder.getBounds(out)

    var scale: Int = 1
    //val sscale get() = RAST_FIXED_SCALE * scale
    //val hscale get() = RAST_FIXED_SCALE_HALF * scale
    val sscale get() = RAST_FIXED_SCALE
    val hscale get() = RAST_FIXED_SCALE_HALF

    @PublishedApi
    internal val Double.s: Int get() = ((this * sscale).toInt() + hscale)
    //@PublishedApi
    //internal val Int.us: Double get() = (this.toDouble() - RAST_FIXED_SCALE_HALF) * scale / RAST_FIXED_SCALE
    //@PublishedApi
    //internal val Int.us2: Double get() = this.toDouble() * scale/ RAST_FIXED_SCALE


    private var closed = true
    private var startPathIndex = 0
    fun reset() {
        closed = true
        startPathIndex = 0
        boundsBuilder.reset()
        pointsX.clear()
        pointsY.clear()
        edges.clear()
    }

    private fun addEdge(ax: Double, ay: Double, bx: Double, by: Double) {
        edges.add(if (ay < by) Edge(ax.s, ay.s, bx.s, by.s, +1) else Edge(bx.s, by.s, ax.s, ay.s, -1))
    }

    private fun addEdge(a: Int, b: Int) {
        if (pointsX[a] == pointsY[a] && pointsX[b] == pointsY[b]) return
        addEdge(pointsX[a], pointsY[a], pointsX[b], pointsY[b])
    }

    val lastX get() = if (pointsX.size > 0) pointsX[pointsX.size - 1] else Double.NEGATIVE_INFINITY
    val lastY get() = if (pointsY.size > 0) pointsY[pointsY.size - 1] else Double.NEGATIVE_INFINITY
    val size get() = pointsX.size

    @PublishedApi
    internal fun addPoint(x: Double, y: Double) {
        if (!closed && x == lastX && y == lastY) return
        //println("ADD($x, $y)")
        pointsX.add(x)
        pointsY.add(y)
        boundsBuilder.add(x, y)
        if (!closed) {
            addEdge(size - 2, size - 1)
        } else {
            closed = false
        }
    }

    inline fun add(x: Number, y: Number) = addPoint(x.toDouble(), y.toDouble())

    inline fun forEachActiveEdgeAtY(y: Int, block: (Edge) -> Unit): Int {
        // @TODO: Optimize this. We can sort edges by Y and perform a binary search?
        var edgesChecked = 0
        for (n in 0 until edges.size) {
            val edge = edges[n]
            edgesChecked++
            if (edge.containsY(y)) {
                block(edge)
            }
        }
        return edgesChecked
    }

    inline fun forEachActiveEdgeAtY(y: Int, near: Int, block: (Edge) -> Unit): Int {
        // @TODO: Optimize this. We can sort edges by Y and perform a binary search?
        var edgesChecked = 0
        for (n in 0 until edges.size) {
            val edge = edges[n]
            edgesChecked++
            if (edge.containsYNear(y, near)) {
                block(edge)
            }
        }
        return edgesChecked
    }

    fun close() {
        //println("CLOSE")
        //add(pointsX[startPathIndex], pointsY[startPathIndex])
        if (size >= 2) {
            add(pointsX[startPathIndex], pointsY[startPathIndex])
        }
        closed = true
        startPathIndex = pointsX.size
    }
    var quality: Int = 2

    data class Stats(
        var edgesChecked: Int = 0,
        var edgesEmitted: Int = 0,
        var yCount: Int = 0
    ) {
        fun reset() {
            edgesChecked = 0
            edgesEmitted = 0
            yCount = 0
        }

        fun chunk(edgesChecked: Int, edgesEmitted: Int, yCount: Int) {
            this.edgesChecked += edgesChecked
            this.edgesEmitted += edgesEmitted
            this.yCount += yCount
        }
    }

    fun rasterizeFill(bounds: Rectangle, quality: Int = this.quality, stats: Stats? = null, callback: RasterizerCallback) {
        stats?.reset()
        val xmin = bounds.left.s
        val xmax = bounds.right.s
        boundsBuilder.getBounds(tempRect)
        val startY = max(bounds.top, tempRect.top).s
        val endY = min(bounds.bottom, tempRect.bottom).s
        val func: (x0: Int, x1: Int, y: Int) -> Unit = { a, b, y ->
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

        yList.clear()
        val q = quality
        //val q = if (quality == 1) 1 else quality + 1
        val yCount = ((((endY - startY) / sscale).toInt()) + 1) * q
        for (n in 0 until yCount + q - 1) {
            val y = (startY + (n * sscale / scale) / q)
            yList.add(y)
        }
        //println("yList: ${yList.size}")

        run {
            var edgesChecked = 0
            var edgesEmitted = 0
            var yCount = 0
            yList.fastForEach { y ->
                yCount++
                tempX.clear()
                edgesChecked += forEachActiveEdgeAtY(y) {
                    if (!it.isCoplanarX) {
                        tempX.add(it.intersectX(y))
                    }
                }
                genericSort(tempX, 0, tempX.size - 1, IntArrayListSort)
                if (tempX.size >= 2) {
                    for (i in 0 until tempX.size - 1 step 2) {
                        val a = tempX[i]
                        val b = tempX[i + 1]
                        func(a, b, y)
                        edgesEmitted++
                    }
                }
            }
            stats?.chunk(edgesChecked, edgesEmitted, yCount)
        }
    }
    private val yList = IntArrayList(1024)
    private val tempX = IntArrayList(1024)

    // @TODO: Change once KDS is updated
    object IntArrayListSort : SortOps<IntArrayList>() {
        override fun compare(subject: IntArrayList, l: Int, r: Int): Int = subject[l].compareTo(subject[r])
        override fun swap(subject: IntArrayList, indexL: Int, indexR: Int) {
            val l = subject[indexL]
            val r = subject[indexR]
            subject[indexR] = l
            subject[indexL] = r
        }
    }

    var strokeWidth: Double = 1.0
}
