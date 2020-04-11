package com.soywiz.korim.font

import com.soywiz.kmem.toIntRound
import com.soywiz.korma.geom.Rectangle
import kotlin.math.roundToInt

data class FontMetrics(
    /** size of the font metric */
    var size: Double = 0.0,
    /** maximum top for any character like É  */
    var top: Double = 0.0,
    /** ascent part of E */
    var ascent: Double = 0.0,
    /** base of 'E' */
    var baseline: Double = 0.0, // Should be 0.0
    /** lower part of 'j' */
    var descent: Double = 0.0,
    /** descent + linegap */
    var bottom: Double = 0.0,
    /** extra height */
    var leading: Double = 0.0,
    /** maximum number of width */
    var maxWidth: Double = 0.0
) {
    /* 'E' height */
    val emHeight get() = ascent - descent
    /* 'É' + 'j' + linegap */
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
        append("emHeight=${emHeight.toIntRound()}, ")
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
    val right: Double get() = bounds.right
    val bottom: Double get() = bounds.bottom
    val left: Double get() = bounds.left
    val top: Double get() = bounds.top
    val width: Double get() = bounds.width
    val height: Double get() = bounds.height

    fun clone() = copy(bounds = bounds.clone())

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

    val right: Double get() = bounds.right
    val bottom: Double get() = bounds.bottom

    val width: Double get() = bounds.width
    val height: Double get() = bounds.height
}
