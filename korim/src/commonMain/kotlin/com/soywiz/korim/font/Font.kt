package com.soywiz.korim.font

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.paint.DefaultPaint
import com.soywiz.korim.vector.paint.Paint
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.transformX
import com.soywiz.korma.geom.transformY

interface Font {
    val name: String

    // Metrics
    fun getFontMetrics(size: Double, metrics: FontMetrics = FontMetrics()): FontMetrics
    fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics = GlyphMetrics()): GlyphMetrics
    fun getKerning(size: Double, leftCodePoint: Int, rightCodePoint: Int): Double

    // Rendering
    fun renderGlyph(ctx: Context2d, size: Double, codePoint: Int, x: Double, y: Double, fill: Boolean, metrics: GlyphMetrics)
}

data class TextToBitmapResult(val bmp: Bitmap, val metrics: TextMetrics)
fun <T> Font.renderTextToBitmap(size: Double, text: T, paint: Paint = DefaultPaint, fill: Boolean = true, renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>): TextToBitmapResult {
    val font = this
    val bounds = getTextBounds(size, text, renderer = renderer)
    val image = NativeImage(bounds.width.toInt(), bounds.height.toInt()).context2d {
        font.drawText(this, size, text, paint, -bounds.left, -bounds.top, fill, renderer = renderer)
    }
    return TextToBitmapResult(image, bounds)
}

fun <T> Font.drawText(ctx: Context2d, size: Double, text: T, paint: Paint, x: Double = 0.0, y: Double = 0.0, fill: Boolean = true, renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>) {
    val actions = object : TextRendererActions() {
        override fun put(codePoint: Int): GlyphMetrics {
            ctx.keepTransform {
                ctx.translate(this.x + x, this.y + y)
                ctx.transform(this.transform)
                ctx.fillStyle = this.paint ?: paint
                font.renderGlyph(ctx, size, codePoint, 0.0, 0.0, true, glyphMetrics)
                if (fill) ctx.fill() else ctx.stroke()
            }
            return glyphMetrics
        }
    }
    renderer(actions, text, size, this)
}
fun <T> Font.getTextBounds(size: Double, text: T, out: TextMetrics = TextMetrics(), renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>): TextMetrics {
    val actions = BoundBuilderTextRendererActions()
    renderer(actions, text, size, this)
    actions.bb.getBounds(out.bounds)
    return out
}

class BoundBuilderTextRendererActions : TextRendererActions() {
    val bb = BoundsBuilder()

    private fun add(x: Double, y: Double) {
        bb.add(this.x + transform.transformX(x, y), this.y + transform.transformY(x, y))
    }

    override fun put(codePoint: Int): GlyphMetrics {
        val g = getGlyphMetrics(codePoint)

        val fx = g.bounds.left
        val fy = g.bounds.top
        val w = g.bounds.width
        val h = -g.bounds.height

        add(fx, fy)
        add(fx + w, fy)
        add(fx + w, fy + h)
        add(fx, fy + h)

        return g
    }
}
