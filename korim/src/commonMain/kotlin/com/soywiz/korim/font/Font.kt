package com.soywiz.korim.font

import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.TextMetrics

interface Font {
    val name: String
    fun getTextBounds(size: Double, text: String, out: TextMetrics = TextMetrics()): TextMetrics
    fun renderText(ctx: Context2d, size: Double, text: String, x: Double, y: Double, fill: Boolean)
}
