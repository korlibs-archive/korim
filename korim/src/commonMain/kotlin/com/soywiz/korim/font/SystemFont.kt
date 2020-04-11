package com.soywiz.korim.font

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.renderer.Renderer

inline class SystemFont(override val name: String) : Font {
    override fun getFontMetrics(size: Double, metrics: FontMetrics): FontMetrics =
        metrics.also { getNativeRenderer().getFontMetrics(this, size, metrics) }

    override fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics): GlyphMetrics =
        metrics.also { getNativeRenderer().getGlyphMetrics(this, size, codePoint, metrics) }

    override fun getKerning(
        size: Double,
        leftCodePoint: Int,
        rightCodePoint: Int
    ): Double = getNativeRenderer().getKerning(this, size, leftCodePoint, rightCodePoint)

    fun getNativeRenderer(): Renderer = NativeImage(1, 1).getContext2d().renderer

    override fun renderGlyph(
        ctx: Context2d,
        size: Double,
        codePoint: Int,
        x: Double,
        y: Double,
        fill: Boolean,
        metrics: GlyphMetrics
    ) {
        val shape = getNativeRenderer().getGlyphShape(this, size, codePoint)
        ctx.keepTransform {
            ctx.translate(x, y)
            ctx.path(shape)
        }
        if (fill) ctx.fill() else ctx.stroke()
    }
}
