package com.soywiz.korim.font

import com.soywiz.korim.vector.Context2d

inline class SystemFont(override val name: String) : Font {
    override fun getFontMetrics(size: Double, metrics: FontMetrics): FontMetrics {
        TODO("Not yet implemented")
    }

    override fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics): GlyphMetrics {
        TODO("Not yet implemented")
    }

    override fun getKerning(
        size: Double,
        leftCodePoint: Int,
        rightCodePoint: Int
    ): Double {
        TODO("Not yet implemented")
    }

    override fun renderGlyph(
        ctx: Context2d,
        size: Double,
        codePoint: Int,
        x: Double,
        y: Double,
        fill: Boolean,
        metrics: GlyphMetrics
    ) {
        ctx.apply {
            ctx.renderer.rendererRenderSystemText(state, this@SystemFont, size, "${codePoint.toChar()}", x, y, fill)
        }
    }
}
