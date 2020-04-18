package com.soywiz.korim.vector

import com.soywiz.korim.internal.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

class FillStrokeTemp {
    private var weight: Double = 1.0
    private lateinit var outFill: VectorPath
    private var startCap: LineCap = LineCap.BUTT
    private var endCap: LineCap = LineCap.BUTT
    private var joins: LineJoin = LineJoin.BEVEL
    internal val strokePoints = PointArrayList()
    internal val rfillPoints = PointArrayList()

    internal fun computeStroke() {
        val weightD2 = weight / 2
        rfillPoints.clear()
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
            val angle = Angle.between(lastX, lastY, x, y)
            val a1 = angle - 45.degrees
            val a2 = angle + 45.degrees
            when {
                first -> {
                }
                else -> {
                    if (n == 1) {
                        outFill.moveTo(lastX + a1.cosine * weightD2, lastY + a1.sine * weightD2)
                        rfillPoints.add(lastX + a2.cosine * weightD2, lastY + a2.sine * weightD2)
                    }
                    outFill.lineTo(x + a1.cosine * weightD2, y + a1.sine * weightD2)
                    if (!last) {
                    }
                    rfillPoints.add(x + a2.cosine * weightD2, y + a2.sine * weightD2)
                }
            }
        }
        // Draw the rest of the points
        for (n in 0 until rfillPoints.size) {
            val m = rfillPoints.size - n - 1
            val x = rfillPoints.getX(m)
            val y = rfillPoints.getY(m)
            outFill.lineTo(x, y)
        }
        outFill.close()
        strokePoints.clear()
    }

    fun set(outFill: VectorPath, weight: Double, startCap: LineCap, endCap: LineCap, joins: LineJoin) {
        this.outFill = outFill
        this.weight = weight
        this.startCap = startCap
        this.endCap = endCap
        this.joins = joins
    }
}

// @TODO: Implement LineCap + LineJoin
fun VectorPath.strokeToFill(
    weight: Double,
    startCap: LineCap,
    endCap: LineCap,
    joins: LineJoin,
    scale: Int = 1,
    temp: FillStrokeTemp = FillStrokeTemp(),
    outFill: VectorPath = VectorPath(winding = Winding.NON_ZERO)
): VectorPath {
    val stroke = this@strokeToFill
    temp.set(outFill, weight, startCap, endCap, joins)
    stroke.emitPoints2 { x, y, move ->
        if (move) temp.computeStroke()
        temp.strokePoints.add(x * scale, y * scale)
    }
    temp.computeStroke()
    return outFill
}
