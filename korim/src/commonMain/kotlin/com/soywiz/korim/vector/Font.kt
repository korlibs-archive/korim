package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.ttf.TtfFont
import com.soywiz.korma.geom.Rectangle
import kotlin.math.max

interface Font {
    val registry: FontRegistry
    val name: String
    val size: Double
    fun clone(name: String = this.name, size: Double = this.size) = registry.get(name, size)
    fun withSize(size: Double): Font
    fun getTextBounds(text: String, out: TextMetrics = TextMetrics()): TextMetrics
    fun renderText(ctx: Context2d, text: String, x: Double, y: Double, fill: Boolean)

    data class System(override val registry: FontRegistry, override val name: String, override val size: Double) : Font {
        override fun withSize(size: Double): Font = copy(size = size)
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
    data class Bitmap(override val registry: FontRegistry, val bitmap: BitmapFont, override val name: String = bitmap.name, override val size: Double) : Font {
        override fun withSize(size: Double): Font = copy(size = size)
        override fun getTextBounds(text: String, out: TextMetrics): TextMetrics {
            var maxx = 0.0
            var maxy = 0.0
            commonProcess(text, handleBounds = { _maxx, _maxy ->
                maxx = _maxx
                maxy = _maxy
            })
            return TextMetrics(Rectangle(0, 0, maxx, maxy))
        }
        override fun renderText(ctx: Context2d, text: String, x: Double, y: Double, fill: Boolean) {
            commonProcess(text, handleGlyph = { x, y, g ->
                ctx.drawImage(g.bmp, x, y)
            })
        }
        private inline fun commonProcess(
            text: String,
            handleGlyph: (x: Double, y: Double, g: BitmapFont.Glyph) -> Unit = { x, y, g -> },
            handleBounds: (maxx: Double, maxy: Double) -> Unit = { maxx, maxy -> }
        ) {
            var x = 0.0
            var y = 0.0
            var maxx = 0.0
            for (c in text) {
                if (c == '\n') {
                    x = 0.0
                    y += bitmap.lineHeight
                } else {
                    val glyph = bitmap[c]
                    handleGlyph(x, y, glyph)
                    x += glyph.xadvance
                    maxx = max(maxx, x + glyph.xadvance)
                }
            }
            handleBounds(maxx, y + bitmap.lineHeight)
        }
    }
    data class Ttf(override val registry: FontRegistry, val ttf: TtfFont, override val name: String = ttf.name, override val size: Double) : Font {
        override fun withSize(size: Double): Font = copy(size = size)
        override fun getTextBounds(text: String, out: TextMetrics): TextMetrics {
            var maxx = 0.0
            var maxy = 0.0
            commonProcess(text, handleBounds = { _maxx, _maxy ->
                maxx = _maxx
                maxy = _maxy
            })
            return TextMetrics(Rectangle(0, 0, maxx, maxy))
        }
        override fun renderText(ctx: Context2d, text: String, x: Double, y: Double, fill: Boolean) {
            commonProcess(text, handleGlyph = { x, y, g ->
                g.draw(ctx, size, origin = TtfFont.Origin.TOP)
                if (fill) ctx.fill() else ctx.stroke()
            })
        }

        private inline fun commonProcess(
            text: String,
            handleGlyph: (x: Double, y: Double, g: TtfFont.IGlyph) -> Unit = { x, y, g -> },
            handleBounds: (maxx: Double, maxy: Double) -> Unit = { maxx, maxy -> }
        ) {
            var x = 0.0
            var y = 0.0
            var maxx = 0.0
            for (c in text) {
                if (c == '\n') {
                    x = 0.0
                    y += ttf.yMax
                } else {
                    val glyph = ttf.getGlyphByChar(c)
                    if (glyph != null) {
                        handleGlyph(x, y, glyph)
                        x += glyph.advanceWidth
                        maxx = max(maxx, x + glyph.advanceWidth)
                    }
                }
            }
            handleBounds(maxx, y + ttf.yMax)
        }
    }
}
