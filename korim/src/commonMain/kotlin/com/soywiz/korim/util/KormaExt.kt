package com.soywiz.korim.util

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

// @TODO: Move this to KorMA
fun VectorBuilder.transformed(m: Matrix): VectorBuilder {
    val im = m.inverted()
    val parent = this
    return object : VectorBuilder {
        override val lastX: Double get() = im.transformX(parent.lastX, parent.lastY)
        override val lastY: Double get() = im.transformY(parent.lastX, parent.lastY)
        override val totalPoints: Int = parent.totalPoints

        fun tX(x: Double, y: Double) = m.transformX(x, y)
        fun tY(x: Double, y: Double) = m.transformY(x, y)

        override fun close() = parent.close()
        override fun lineTo(x: Double, y: Double) = parent.lineTo(tX(x, y), tY(x, y))
        override fun moveTo(x: Double, y: Double) = parent.lineTo(tX(x, y), tY(x, y))
        override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) = parent.quadTo(
            tX(cx, cy), tY(cx, cy),
            tX(ax, ay), tY(ax, ay)
        )
        override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) = parent.cubicTo(
            tX(cx1, cy1), tY(cx1, cy1),
            tX(cx2, cy2), tY(cx2, cy2),
            tX(ax, ay), tY(ax, ay)
        )
    }
}

fun <T> VectorBuilder.transformed(m: Matrix, block: VectorBuilder.() -> T): T = block(this.transformed(m))
