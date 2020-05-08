package com.soywiz.korim.vector.rasterizer

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korim.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

const val RAST_FIXED_SCALE = 32 // Important NOTE: Power of two so divisions are >> and remaining &
//const val RAST_FIXED_SCALE = 20 // Important NOTE: Power of two so divisions are >> and remaining &
const val RAST_FIXED_SCALE_HALF = (RAST_FIXED_SCALE / 2) - 1
//const val RAST_FIXED_SCALE_HALF = (RAST_FIXED_SCALE / 2)
//const val RAST_FIXED_SCALE_HALF = 0

open class RastScale {
    val sscale get() = RAST_FIXED_SCALE
    val hscale get() = RAST_FIXED_SCALE_HALF

    @PublishedApi
    internal val Double.s: Int get() = ((this * sscale).toInt() + hscale)
    //@PublishedApi
    //internal val Int.us: Double get() = (this.toDouble() - RAST_FIXED_SCALE_HALF) * scale / RAST_FIXED_SCALE
    //@PublishedApi
    //internal val Int.us2: Double get() = this.toDouble() * scale/ RAST_FIXED_SCALE

}

class PolygonScanline : RastScale() {
    var winding = Winding.NON_ZERO
    private val boundsBuilder = BoundsBuilder()

    private val edgesPool = Pool { Edge() }

    @PublishedApi
    internal val edges = arrayListOf<Edge>()

    fun getBounds(out: Rectangle = Rectangle()) = boundsBuilder.getBounds(out)

    private var closed = true
    fun reset() {
        closed = true
        boundsBuilder.reset()
        edges.fastForEach { edgesPool.free(it) }
        edges.clear()
    }

    private fun addEdge(ax: Double, ay: Double, bx: Double, by: Double) {
        if (ax == bx && ay == by) return
        if (ay == by) return // Do not add coplanar to X edges
        edges.add(if (ay < by) edgesPool.alloc().setTo(ax.s, ay.s, bx.s, by.s, +1) else edgesPool.alloc().setTo(bx.s, by.s, ax.s, ay.s, -1))
        boundsBuilder.add(ax, ay)
        boundsBuilder.add(bx, by)
    }

    var moveToX = 0.0
    var moveToY = 0.0
    var lastX = 0.0
    var lastY = 0.0
    val edgesSize get() = edges.size
    fun isNotEmpty() = edgesSize > 0

    fun moveTo(x: Double, y: Double) {
        lastX = x
        lastY = y
        moveToX = x
        moveToY = y
    }

    fun lineTo(x: Double, y: Double) {
        addEdge(lastX, lastY, x, y)
        lastX = x
        lastY = y
    }

    inline fun add(x: Number, y: Number, move: Boolean) = if (move) moveTo(x.toDouble(), y.toDouble()) else lineTo(x.toDouble(), y.toDouble())

    internal inline fun forEachActiveEdgeAtY(y: Int, block: (Edge) -> Unit): Int {
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

    internal inline fun forEachActiveEdgeAtY(y: Int, near: Int, block: (Edge) -> Unit): Int {
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
        lineTo(moveToX, moveToY)
    }

    private val tempXW = XWithWind()

    var edgesChecked = 0
    fun scanline(y: Int, winding: Winding, out: SegmentSet = SegmentSet()): SegmentSet {
        edgesChecked = 0

        tempXW.clear()
        out.clear()
        edgesChecked += forEachActiveEdgeAtY(y) {
            if (!it.isCoplanarX) {
                tempXW.add(it.intersectX(y), it.wind)
            }
        }
        genericSort(tempXW, 0, tempXW.size - 1, IntArrayListSort)
        //tempXW.removeXDuplicates()
        val tempX = tempXW.x
        val tempW = tempXW.w
        if (tempXW.size >= 2) {
            //println(winding)
            //val winding = Winding.NON_ZERO
            when (winding) {
                Winding.EVEN_ODD -> {
                    //println(y)
                    //if (y == 2999) println("STEP: $tempX, $tempXW")
                    for (i in 0 until tempX.size - 1 step 2) {
                        val a = tempX.getAt(i)
                        val b = tempX.getAt(i + 1)
                        out.add(a, b)
                        //if (y == 2999) println("STEP: $a, $b")
                    }
                }
                Winding.NON_ZERO -> {
                    //println("NON-ZERO")

                    var count = 0
                    var startX = 0
                    var endX = 0
                    var pending = false

                    for (i in 0 until tempX.size - 1) {
                        val a = tempX.getAt(i)
                        count += tempW.getAt(i)
                        val b = tempX.getAt(i + 1)
                        if (count != 0) {
                            if (pending && a != endX) {
                                out.add(startX, endX)
                                startX = a
                                endX = b
                            } else {
                                if (!pending) {
                                    startX = a
                                }
                                endX = b
                            }
                            //func(a, b, y)
                            pending = true
                        }
                    }

                    if (pending) {
                        out.add(startX, endX)
                    }
                }
            }
        }
        return out
    }


    private class XWithWind {
        val x = IntArrayList(1024)
        val w = IntArrayList(1024)
        val size get() = x.size

        fun add(x: Int, wind: Int) {
            this.x.add(x)
            this.w.add(wind)
        }

        fun clear() {
            x.clear()
            w.clear()
        }

        fun removeXDuplicates() {
            genericRemoveSortedDuplicates(
                size = size,
                equals = { x, y -> this.x.getAt(x) == this.x.getAt(y) },
                copy = { src, dst ->
                    this.x[dst] = this.x.getAt(src)
                    this.w[dst] = this.w.getAt(src)
                },
                resize = { size ->
                    this.x.size = size
                    this.w.size = size
                }
            )
        }

        override fun toString(): String = "XWithWind($x, $w)"
    }

    // @TODO: Change once KDS is updated
    private object IntArrayListSort : SortOps<XWithWind>() {
        override fun compare(subject: XWithWind, l: Int, r: Int): Int = subject.x.getAt(l).compareTo(subject.x.getAt(r))
        override fun swap(subject: XWithWind, indexL: Int, indexR: Int) {
            subject.x.swap(indexL, indexR)
            subject.w.swap(indexL, indexR)
        }
    }
}

private fun IntArrayList.swap(x: Int, y: Int) {
    val l = this.getAt(x)
    val r = this.getAt(y)
    this[x] = r
    this[y] = l
}
