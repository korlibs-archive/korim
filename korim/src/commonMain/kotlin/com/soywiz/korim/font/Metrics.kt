package com.soywiz.korim.font

import com.soywiz.kmem.toIntRound
import com.soywiz.korma.geom.Rectangle
import kotlin.math.roundToInt

data class FontMetrics(
    var size: Double = 0.0,
    var top: Double = 0.0,
    var ascent: Double = 0.0,
    var baseline: Double = 0.0, // Should be 0.0
    var descent: Double = 0.0,
    var bottom: Double = 0.0,
    var leading: Double = 0.0
) {
    val lineHeight get() = top - bottom

    override fun toString(): String = buildString {
        append("FontMetrics(")
        append("size=${size.toIntRound()}, ")
        append("top=${top.toIntRound()}, ")
        append("ascent=${ascent.toIntRound()}, ")
        append("baseline=${baseline.toIntRound()}, ")
        append("descent=${descent.toIntRound()}, ")
        append("bottom=${bottom.toIntRound()}, ")
        append("leading=${leading.toIntRound()}, ")
        append("lineHeight=${lineHeight.toIntRound()}")
        append(")")
    }
}

data class GlyphMetrics(
    var existing: Boolean = false,
    var codePoint: Int = 0,
    val bounds: Rectangle = Rectangle(),
    var xadvance: Double = 0.0
) {
    val left: Double get() = bounds.left
    val top: Double get() = bounds.top
    val width: Double get() = bounds.width
    val height: Double get() = bounds.height

    override fun toString(): String = buildString {
        append("GlyphMetrics(")
        append("codePoint=${codePoint} ('${codePoint.toChar()}'), ")
        append("existing=$existing, ")
        append("xadvance=${xadvance.roundToInt()}, ")
        append("bounds=${bounds.toInt()}")
        append(")")
    }
}

data class TextMetrics(
    val bounds: Rectangle = Rectangle()
) {
    val left: Double get() = bounds.left
    val top: Double get() = bounds.top
    val width: Double get() = bounds.width
    val height: Double get() = bounds.height
}
