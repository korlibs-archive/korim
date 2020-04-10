package com.soywiz.korim.font

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.font.ttf.TtfFont
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.TextMetrics
import com.soywiz.korma.geom.Rectangle
import kotlin.math.max

interface Font {
    val registry: FontRegistry
    val name: String
    val size: Double
    fun getTextBounds(text: String, out: TextMetrics = TextMetrics()): TextMetrics
    fun renderText(ctx: Context2d, text: String, x: Double, y: Double, fill: Boolean)
}

fun Font.clone(name: String = this.name, size: Double = this.size) = registry.get(name, size)
fun Font.withSize(size: Double): Font = clone(this.name, size)
