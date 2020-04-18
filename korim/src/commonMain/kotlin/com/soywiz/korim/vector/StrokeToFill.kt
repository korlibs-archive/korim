package com.soywiz.korim.vector

import com.soywiz.korim.internal.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

class FillStrokeTemp {
    val points = PointArrayList()
    fun reset() {
        points.clear()
    }
}

// @TODO: Implement LineCap + LineJoin
fun VectorPath.getFilledStroke(
    weight: Double,
    startCap: LineCap,
    endCap: LineCap,
    joins: LineJoin,
    scale: Int = 1,
    temp: FillStrokeTemp = FillStrokeTemp(),
    outFill: VectorPath = VectorPath()
): VectorPath {
    val stroke = this@getFilledStroke
    var count = 0
    val weightD2 = weight * scale / 2
    val points = temp.points
    temp.reset()

    fun flush() {
        if (points.size <= 0) return
        for (n in 0 until points.size) {
            val m = points.size - n - 1
            val x = points.getX(m)
            val y = points.getY(m)
            outFill.lineTo(x, y)
        }
        outFill.close()
        points.clear()
    }

    stroke.emitPoints2({ close ->
        if (close && points.isNotEmpty()) {
            //println("CLOSE")
            //points.add(points.getX(0), points.getY(0))
        } else {
            //println("FINISH")
        }
    }, { x, y, move ->
        if (move) {
            flush()
            count = 0
        }
        val x = x * scale
        val y = y * scale
        if (count > 0) {
            val angle = Angle.between(lastX, lastY, x, y)
            val a1 = angle - 45.degrees
            val a2 = angle + 45.degrees
            if (count == 1) {
                outFill.moveTo(lastX + a1.cosine * weightD2, lastY + a1.sine * weightD2)
            }
            outFill.lineTo(x + a1.cosine * weightD2, y + a1.sine * weightD2)
            points.add(lastX + a2.cosine * weightD2, lastY + a2.sine * weightD2)
            points.add(x + a2.cosine * weightD2, y + a2.sine * weightD2)
        }
        lastX = x
        lastY = y
        count++
    })
    flush()

    return outFill
}
