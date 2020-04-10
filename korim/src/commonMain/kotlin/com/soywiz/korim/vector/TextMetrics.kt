package com.soywiz.korim.vector

import com.soywiz.korma.geom.Rectangle

data class TextMetrics(
    val bounds: Rectangle = Rectangle(),
    var base: Double = 0.0
) {
    val left: Double get() = bounds.left
    val top: Double get() = bounds.top
    val width: Double get() = bounds.width
    val height: Double get() = bounds.height
}
