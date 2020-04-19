package com.soywiz.korim.vector

import com.soywiz.korim.internal.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

class FillStrokeTemp {
    private var weight: Double = 2.0
    private lateinit var outFill: VectorPath
    private var startCap: LineCap = LineCap.BUTT
    private var endCap: LineCap = LineCap.BUTT
    private var joins: LineJoin = LineJoin.BEVEL
    private var miterLimit: Double = 4.0
    internal val strokePoints = PointArrayList(1024)
    internal val fillPoints = Array(2) { PointArrayList(1024) }

    internal fun computeStroke() {
        val weightD2 = weight / 2
        fillPoints[0].clear()
        fillPoints[1].clear()
        val nstrokePoints = strokePoints.size
        for (n in 0 until nstrokePoints) {
            val first = n == 0
            val last = n == nstrokePoints - 1
            val middle = !first && !last
            val lastX = if (first) 0.0 else strokePoints.getX(n - 1)
            val lastY = if (first) 0.0 else strokePoints.getY(n - 1)
            val x = strokePoints.getX(n)
            val y = strokePoints.getY(n)
            val nextX = if (last) 0.0 else strokePoints.getX(n + 1)
            val nextY = if (last) 0.0 else strokePoints.getY(n + 1)
            val prevAngle = Angle.between(lastX, lastY, x, y)
            val pa1 = prevAngle - 90.degrees
            val pa2 = prevAngle + 90.degrees
            val nextAngle = Angle.between(x, y, nextX, nextY)
            val na1 = nextAngle - 90.degrees
            val na2 = nextAngle + 90.degrees
            if (first) {
                fillPoints[0].add(x + na1.cosine * weightD2, y + na1.sine * weightD2)
                fillPoints[1].add(x + na2.cosine * weightD2, y + na2.sine * weightD2)
            } else {
                fillPoints[0].add(x + pa1.cosine * weightD2, y + pa1.sine * weightD2)
                if (!last) {
                    val n = if (prevAngle - nextAngle < 0.degrees) 0 else 1
                    fillPoints[n].add(x + na1.cosine * weightD2, y + na1.sine * weightD2)
                }
                fillPoints[1].add(x + pa2.cosine * weightD2, y + pa2.sine * weightD2)
            }
        }
        for (n in 0 until fillPoints[0].size) {
            val x = fillPoints[0].getX(n)
            val y = fillPoints[0].getY(n)
            if (n == 0) {
                outFill.moveTo(x, y)
            } else {
                outFill.lineTo(x, y)
            }
        }
        // Draw the rest of the points
        for (n in 0 until fillPoints[1].size) {
            val m = fillPoints[1].size - n - 1
            val x = fillPoints[1].getX(m)
            val y = fillPoints[1].getY(m)
            outFill.lineTo(x, y)
        }
        outFill.close()
        strokePoints.clear()
    }

    fun set(outFill: VectorPath, weight: Double, startCap: LineCap, endCap: LineCap, joins: LineJoin, miterLimit: Double) {
        this.outFill = outFill
        this.weight = weight
        this.startCap = startCap
        this.endCap = endCap
        this.joins = joins
        this.miterLimit = miterLimit
    }
}

// @TODO: Implement LineCap + LineJoin
fun VectorPath.strokeToFill(
    lineWidth: Double,
    joins: LineJoin = LineJoin.MITER,
    startCap: LineCap = LineCap.BUTT,
    endCap: LineCap = startCap,
    miterLimit: Double = 4.0,
    scale: Int = 1,
    temp: FillStrokeTemp = FillStrokeTemp(),
    outFill: VectorPath = VectorPath(winding = Winding.NON_ZERO)
): VectorPath {
    val stroke = this@strokeToFill
    temp.set(outFill, lineWidth, startCap, endCap, joins, miterLimit)
    stroke.emitPoints2 { x, y, move ->
        if (move) temp.computeStroke()
        temp.strokePoints.add(x * scale, y * scale)
    }
    temp.computeStroke()
    return outFill
}
