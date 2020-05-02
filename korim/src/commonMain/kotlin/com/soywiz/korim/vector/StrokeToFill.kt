package com.soywiz.korim.vector

import com.soywiz.kmem.*
import com.soywiz.korim.internal.*
import com.soywiz.korim.vector.rasterizer.*
import com.soywiz.korim.vector.rasterizer.Edge
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.vector.*

class FillStrokeTemp {
    private var weight: Int = 1
    private lateinit var outFill: VectorPath
    private var startCap: LineCap = LineCap.BUTT
    private var endCap: LineCap = LineCap.BUTT
    private var joins: LineJoin = LineJoin.BEVEL
    private var miterLimit: Double = 4.0 // ratio of the width
    internal val strokePoints = PointIntArrayList(1024)
    internal val fillPoints = Array(2) { PointIntArrayList(1024) }
    internal val fillPointsLeft = fillPoints[0]
    internal val fillPointsRight = fillPoints[1]

    private val prevEdge = Edge()
    private val prevEdgeLeft = Edge()
    private val prevEdgeRight = Edge()

    private val currEdge = Edge()
    private val currEdgeLeft = Edge()
    private val currEdgeRight = Edge()

    internal fun Edge.setEdgeDisplaced(edge: Edge, width: Int, angle: Angle) = this.apply {
        val ldx = (width * angle.cosine)
        val ldy = (width * angle.sine)
        this.setTo((edge.ax + ldx).toInt(), (edge.ay + ldy).toInt(), (edge.bx + ldx).toInt(), (edge.by + ldy).toInt(), edge.wind)
    }

    internal fun PointIntArrayList.addEdgePointA(e: Edge) = add(e.ax, e.ay)
    internal fun PointIntArrayList.addEdgePointB(e: Edge) = add(e.bx, e.by)
    internal fun PointIntArrayList.add(e: Point?) = run { if (e != null) add(e.x.toInt(), e.y.toInt()) }
    internal fun PointIntArrayList.add(x: Double, y: Double) = run { add(x.toInt(), y.toInt()) }

    private val tempP1 = Point()
    private val tempP2 = Point()
    private val tempP3 = Point()

    internal fun doJoin(out: PointIntArrayList, mainPrev: Edge, mainCurr: Edge, prev: Edge, curr: Edge, join: LineJoin, miterLimit: Double, scale: Double, forcedMiter: Boolean) {
        val rjoin = if (forcedMiter) LineJoin.MITER else join
        when (rjoin) {
            LineJoin.MITER -> {
                val intersection2 = tempP1.setTo(mainPrev.bx, mainPrev.by)
                val intersection = Edge.getIntersectXY(prev, curr, tempP3)
                if (intersection != null) {
                    val dist = Point.distance(intersection, intersection2)
                    if (forcedMiter || dist <= miterLimit) {
                        out.add(intersection)
                    } else {
                        out.addEdgePointB(prev)
                        out.addEdgePointA(curr)
                    }
                }
            }
            LineJoin.BEVEL -> {
                out.addEdgePointB(prev)
                out.addEdgePointA(curr)
            }
            LineJoin.ROUND -> {
                val i = Edge.getIntersectXY(prev, curr, tempP3)
                if (i != null) {
                    val count = (Point.distance(prev.bx, prev.by, curr.ax, curr.ay) * scale).toInt().clamp(4, 64)
                    for (n in 0..count) {
                        out.add(Bezier.quadCalc(prev.bx.toDouble(), prev.by.toDouble(), i.x, i.y, curr.ax.toDouble(), curr.ay.toDouble(), n.toDouble() / count, tempP2))
                    }
                } else {
                    out.addEdgePointB(prev)
                    out.addEdgePointA(curr)
                }
            }
        }
    }

    internal fun computeStroke(scale: Double) {
        val weightD2 = weight / 2
        fillPointsLeft.clear()
        fillPointsRight.clear()
        val sp = strokePoints
        val nstrokePoints = sp.size

        for (n in 0 until nstrokePoints) {
            val isFirst = n == 0
            val isLast = n == nstrokePoints - 1
            val isMiddle = !isFirst && !isLast
            val n1 = if (isLast) n else n + 1

            prevEdge.copyFrom(currEdge)
            prevEdgeLeft.copyFrom(currEdgeLeft)
            prevEdgeRight.copyFrom(currEdgeRight)

            currEdge.setTo(sp.getX(n), sp.getY(n), sp.getX(n1), sp.getY(n1), +1)
            currEdgeLeft.setEdgeDisplaced(currEdge, weightD2, currEdge.angle - 90.degrees)
            currEdgeRight.setEdgeDisplaced(currEdge, weightD2, currEdge.angle + 90.degrees)

            when {
                isFirst -> {
                    fillPointsLeft.addEdgePointA(currEdgeLeft)
                    fillPointsRight.addEdgePointA(currEdgeRight)
                }
                isMiddle -> {
                    val angle = Edge.angleBetween(prevEdge, currEdge)
                    val leftAngle = angle > 0.degrees

                    doJoin(fillPointsLeft, prevEdge, currEdge, prevEdgeLeft, currEdgeLeft, joins, miterLimit, scale, leftAngle)
                    doJoin(fillPointsRight, prevEdge, currEdge, prevEdgeRight, currEdgeRight, joins, miterLimit, scale, !leftAngle)
                }
                isLast -> {
                    fillPointsLeft.addEdgePointB(prevEdgeLeft)
                    fillPointsRight.addEdgePointB(prevEdgeRight)
                }
            }
        }

        for (n in 0 until fillPointsLeft.size) {
            val x = fillPointsLeft.getX(n)
            val y = fillPointsLeft.getY(n)
            if (n == 0) {
                outFill.moveTo(x * scale, y * scale)
            } else {
                outFill.lineTo(x * scale, y * scale)
            }
        }
        // Draw the rest of the points
        for (n in 0 until fillPointsRight.size) {
            val m = fillPointsRight.size - n - 1
            outFill.lineTo(fillPointsRight.getX(m) * scale, fillPointsRight.getY(m) * scale)
        }
        outFill.close()
        strokePoints.clear()
    }

    fun set(outFill: VectorPath, weight: Int, startCap: LineCap, endCap: LineCap, joins: LineJoin, miterLimit: Double) {
        this.outFill = outFill
        this.weight = weight
        this.startCap = startCap
        this.endCap = endCap
        this.joins = joins
        this.miterLimit = miterLimit * weight
    }

    fun strokeFill(
        stroke: VectorPath,
        lineWidth: Double, joins: LineJoin, startCap: LineCap, endCap: LineCap, miterLimit: Double, outFill: VectorPath
    ) {
        val scale = RAST_FIXED_SCALE
        val iscale = 1.0 / RAST_FIXED_SCALE
        set(outFill, (lineWidth * scale).toInt(), startCap, endCap, joins, miterLimit)
        stroke.emitPoints2 { x, y, move ->
            if (move) computeStroke(iscale)
            strokePoints.add((x * scale).toInt(), (y * scale).toInt())
        }
        computeStroke(iscale)
    }
}

// @TODO: Implement LineCap + LineJoin
fun VectorPath.strokeToFill(
    lineWidth: Double,
    joins: LineJoin = LineJoin.MITER,
    startCap: LineCap = LineCap.BUTT,
    endCap: LineCap = startCap,
    miterLimit: Double = 4.0,
    temp: FillStrokeTemp = FillStrokeTemp(),
    outFill: VectorPath = VectorPath(winding = Winding.NON_ZERO)
): VectorPath {
    temp.strokeFill(
        this@strokeToFill, lineWidth, joins, startCap, endCap, miterLimit, outFill
    )
    return outFill
}
