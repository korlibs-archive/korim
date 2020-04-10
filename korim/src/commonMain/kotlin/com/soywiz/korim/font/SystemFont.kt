package com.soywiz.korim.font

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.TextMetrics

inline class SystemFont(override val name: String) : Font {
    override fun getTextBounds(size: Double, text: String, out: TextMetrics): TextMetrics {
        val bni = NativeImage(1, 1)
        val bnictx = bni.getContext2d()
        bnictx.renderer.getBounds(this, size, text, out)
        return out
    }
    override fun renderText(ctx: Context2d, size: Double, text: String, x: Double, y: Double, fill: Boolean) {
        ctx.apply {
            ctx.renderer.rendererRenderSystemText(state, this@SystemFont, size, text, x, y, fill)
        }
    }
}
