package com.soywiz.korim.font

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.TextMetrics

data class SystemFont(override val name: String, override val size: Double, override val registry: FontRegistry) :
    Font {
    override fun getTextBounds(text: String, out: TextMetrics): TextMetrics {
        val bni = NativeImage(1, 1)
        val bnictx = bni.getContext2d()
        bnictx.renderer.getBounds(this, text, out)
        return out
    }
    override fun renderText(ctx: Context2d, text: String, x: Double, y: Double, fill: Boolean) {
        ctx.apply {
            ctx.renderer.rendererRenderSystemText(state, font, text, x, y, fill)
        }
    }
}

inline fun SystemFont(name: String, size: Number) = SystemFont(name, size.toDouble(), SystemFontRegistry)
