package com.soywiz.korim.vector.rasterizer

import com.soywiz.kds.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

const val RAST_FIXED_SCALE = 32 // Important NOTE: Power of two so divisions are >> and remaining &
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
    val boundsBuilder = BoundsBuilder()
    private val points = PointArrayList(1024)

    @PublishedApi
    internal val edges = arrayListOf<Edge>()

    fun getBounds(out: Rectangle = Rectangle()) = boundsBuilder.getBounds(out)

    private var closed = true
    private var startPathIndex = 0
    fun reset() {
        closed = true
        startPathIndex = 0
        boundsBuilder.reset()
        points.clear()
        edges.clear()
    }

    private fun addEdge(ax: Double, ay: Double, bx: Double, by: Double) {
        edges.add(if (ay < by) Edge(ax.s, ay.s, bx.s, by.s, +1) else Edge(bx.s, by.s, ax.s, ay.s, -1))
    }

    private fun addEdge(a: Int, b: Int) {
        if ((points.getX(a) == points.getX(b)) && (points.getY(a) == points.getY(b))) return
        addEdge(points.getX(a), points.getY(a), points.getX(b), points.getY(b))
    }

    val lastX get() = if (points.size > 0) points.getX(points.size - 1) else Double.NEGATIVE_INFINITY
    val lastY get() = if (points.size > 0) points.getY(points.size - 1) else Double.NEGATIVE_INFINITY
    val size get() = points.size

    @PublishedApi
    internal fun addPoint(x: Double, y: Double) {
        if (!closed && x == lastX && y == lastY) return
        //println("ADD($x, $y)")
        points.add(x, y)
        boundsBuilder.add(x, y)
        if (!closed) {
            addEdge(size - 2, size - 1)
        } else {
            closed = false
        }
    }

    inline fun add(x: Number, y: Number) = addPoint(x.toDouble(), y.toDouble())

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
        if (size >= 2) {
            add(points.getX(startPathIndex), points.getY(startPathIndex))
        }
        closed = true
        startPathIndex = points.size
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
        val tempX = tempXW.x
        val tempW = tempXW.w
        if (tempXW.size >= 2) {
            when (winding) {
                Winding.EVEN_ODD -> {
                    for (i in 0 until tempX.size - 1 step 2) {
                        val a = tempX.getAt(i)
                        val b = tempX.getAt(i + 1)
                        out.add(a, b)
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
