package com.soywiz.korim.internal

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.vector.*
import kotlin.collections.get
import kotlin.math.*

@PublishedApi
internal inline fun approximateCurve(
    curveSteps: Int,
    crossinline compute: (ratio: Double, get: (x: Double, y: Double) -> Unit) -> Unit,
    crossinline emit: (x: Double, y: Double) -> Unit
) {
    val rcurveSteps = max(curveSteps, 20)
    val dt = 1.0 / rcurveSteps
    var lastX = 0.0
    var lastY = 0.0
    var prevX = 0.0
    var prevY = 0.0
    var emittedCount = 0
    compute(0.0) { x, y ->
        lastX = x
        lastY = y
    }
    for (n in 1 until rcurveSteps) {
        val ratio = n * dt
        //println("ratio: $ratio")
        compute(ratio) { x, y ->
            //if (emittedCount == 0) {
            run {
                emit(x, y)
                emittedCount++
                lastX = prevX
                lastY = prevY
            }

            prevX = x
            prevY = y
        }
    }
    //println("curveSteps: $rcurveSteps, emittedCount=$emittedCount")
}

internal inline fun VectorPath.emitPoints2(crossinline flush: (close: Boolean) -> Unit = {}, crossinline emit: (x: Double, y: Double, move: Boolean) -> Unit) {
    var lx = 0.0
    var ly = 0.0
    flush(false)
    this.visitCmds(
        moveTo = { x, y -> emit(x, y, true).also { lx = x }.also { ly = y } },
        lineTo = { x, y -> emit(x, y, false).also { lx = x }.also { ly = y } },
        quadTo = { x0, y0, x1, y1 ->
            val sum = Point.distance(lx, ly, x0, y0) + Point.distance(x0, y0, x1, y1)
            approximateCurve(sum.toInt(), { ratio, get -> Bezier.quadCalc(lx, ly, x0, y0, x1, y1, ratio) { x, y -> get(x, y) } }) { x, y -> emit(x, y, false) }
            run { lx = x1 }.also { ly = y1 }
        },
        cubicTo = { x0, y0, x1, y1, x2, y2 ->
            val sum = Point.distance(lx, ly, x0, y0) + Point.distance(x0, y0, x1, y1) + Point.distance(x1, y1, x2, y2)
            approximateCurve(sum.toInt(), { ratio, get -> Bezier.cubicCalc(lx, ly, x0, y0, x1, y1, x2, y2, ratio) { x, y -> get(x, y) }}) { x, y -> emit(x, y, false) }
            run { lx = x2 }.also { ly = y2 }

        },
        close = { flush(true) }
    )
    flush(false)
}
