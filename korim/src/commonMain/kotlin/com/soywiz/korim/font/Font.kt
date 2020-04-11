package com.soywiz.korim.font

import com.soywiz.kmem.toIntCeil
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.paint.DefaultPaint
import com.soywiz.korim.vector.paint.Paint
import com.soywiz.korma.geom.*

interface Font {
    val name: String

    // Metrics
    fun getFontMetrics(size: Double, metrics: FontMetrics = FontMetrics()): FontMetrics
    fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics = GlyphMetrics()): GlyphMetrics
    fun getKerning(size: Double, leftCodePoint: Int, rightCodePoint: Int): Double

    // Rendering
    fun renderGlyph(ctx: Context2d, size: Double, codePoint: Int, x: Double, y: Double, fill: Boolean, metrics: GlyphMetrics)
}

data class TextToBitmapResult(
    val bmp: Bitmap32,
    val fmetrics: FontMetrics,
    val metrics: TextMetrics,
    val glyphs: List<PlacedGlyph>
) {
    data class PlacedGlyph(val codePoint: Int, val x: Double, val y: Double, val metrics: GlyphMetrics, val transform: Matrix)
}

fun Font.renderGlyphToBitmap(size: Double, codePoint: Int, paint: Paint = DefaultPaint, fill: Boolean = true): TextToBitmapResult {
    val font = this
    val fmetrics = getFontMetrics(size)
    val gmetrics = getGlyphMetrics(size, codePoint)
    val gx = -gmetrics.left
    val gy = gmetrics.height + gmetrics.top
    val bmp = NativeImage(gmetrics.width.toIntCeil(), gmetrics.height.toIntCeil()).context2d {
        fillStyle = paint
        font.renderGlyph(this, size, codePoint, gx, gy, fill = true, metrics = gmetrics)
        if (fill) fill() else stroke()
    }
    return TextToBitmapResult(bmp.toBMP32().premultipliedIfRequired(), fmetrics, TextMetrics(), listOf(
        TextToBitmapResult.PlacedGlyph(codePoint, gx, gy, gmetrics, Matrix())
    ))
}

// @TODO: Fix metrics
fun <T> Font.renderTextToBitmap(size: Double, text: T, paint: Paint = DefaultPaint, fill: Boolean = true, renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>, returnGlyphs: Boolean = true): TextToBitmapResult {
    val font = this
    val bounds = getTextBounds(size, text, renderer = renderer)
    //println("BOUNDS: $bounds")
    val glyphs = arrayListOf<TextToBitmapResult.PlacedGlyph>()
    val iwidth = bounds.width.toInt()
    val iheight = bounds.height.toInt()
    val image = NativeImage(iwidth, iheight).context2d {
    //val image = Bitmap32(iwidth, iheight).context2d {
        font.drawText(this, size, text, paint, -bounds.left, -bounds.top, fill, renderer = renderer, placed = { codePoint, x, y, size, metrics, transform ->
            if (returnGlyphs) {
                glyphs += TextToBitmapResult.PlacedGlyph(codePoint, x, y, metrics.clone(), transform.clone())
            }
        })
    }
    return TextToBitmapResult(image.toBMP32(), font.getFontMetrics(size), bounds, glyphs)
}

fun <T> Font.drawText(
    ctx: Context2d, size: Double,
    text: T, paint: Paint,
    x: Double = 0.0, y: Double = 0.0,
    fill: Boolean = true,
    renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
    placed: ((codePoint: Int, x: Double, y: Double, size: Double, metrics: GlyphMetrics, transform: Matrix) -> Unit)? = null
) {
    val actions = object : TextRendererActions() {
        override fun put(codePoint: Int): GlyphMetrics {
            ctx.keepTransform {
                ctx.translate(this.x + x, this.y + y)
                ctx.transform(this.transform)
                ctx.fillStyle = this.paint ?: paint
                font.renderGlyph(ctx, size, codePoint, 0.0, 0.0, true, glyphMetrics)
                placed?.invoke(codePoint, this.x + x, this.y + y, size, glyphMetrics, this.transform)
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
        //val itransform = transform.inverted()
        val rx = this.x + transform.transformX(x, y)
        val ry = this.y + transform.transformY(x, y)
        //println("P: $rx, $ry [$x, $y]")
        bb.add(rx, ry)
    }

    override fun put(codePoint: Int): GlyphMetrics {
        val g = getGlyphMetrics(codePoint)

        val fx = g.bounds.left
        val fy = g.bounds.top
        val w = g.bounds.width
        val h = -g.bounds.height

        //println("------: [$x,$y] -- ($fx, $fy)-($w, $h)")
        add(fx, fy)
        add(fx + w, fy)
        add(fx + w, fy + h)
        add(fx, fy + h)

        return g
    }
}
