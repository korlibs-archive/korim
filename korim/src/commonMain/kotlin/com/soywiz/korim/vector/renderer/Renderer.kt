package com.soywiz.korim.vector.renderer

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.font.*
import com.soywiz.korim.util.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

abstract class Renderer {
    var debug: Boolean = false
    abstract val width: Int
    abstract val height: Int

    inline fun <T> buffering(callback: () -> T): T {
        bufferingStart()
        try {
            return callback()
        } finally {
            bufferingEnd()
        }
    }

    private var bufferingLevel = 0
    protected fun isBuffering() = bufferingLevel > 0
    open protected fun flush() = Unit
    fun bufferingStart() = bufferingLevel++
    fun bufferingEnd() {
        bufferingLevel--
        if (bufferingLevel == 0) {
            flush()
        }
    }
    open fun render(state: Context2d.State, fill: Boolean): Unit = Unit
    open fun getGlyphShape(systemFont: SystemFont, size: Double, codePoint: Int): GraphicsPath = GraphicsPath()
    //open fun renderText(state: Context2d.State, font: Font, fontSize: Double, text: String, x: Double, y: Double, fill: Boolean): Unit = Unit
    //open fun getBounds(font: Font, fontSize: Double, text: String, out: TextMetrics): Unit = run { out.bounds.setTo(0.0, 0.0, 0.0, 0.0) }
    open fun getKerning(systemFont: SystemFont, size: Double, c1: Int, c2: Int): Double = 0.0
    open fun getGlyphMetrics(systemFont: SystemFont, size: Double, codePoint: Int, metrics: GlyphMetrics) {
        metrics.existing = true
        metrics.codePoint = codePoint
        metrics.bounds.setTo(0, 0, size, size)
        metrics.xadvance = size
    }
    open fun getFontMetrics(systemFont: SystemFont, size: Double, metrics: FontMetrics) {
        metrics.size = size
        metrics.top = size
        metrics.ascent = size
        metrics.baseline = 0.0
        metrics.descent = 0.0
        metrics.bottom = 0.0
        metrics.leading = 0.0
        metrics.maxWidth = size
    }

    open fun drawImage(
        image: Bitmap,
        x: Double,
        y: Double,
        width: Double = image.width.toDouble(),
        height: Double = image.height.toDouble(),
        transform: Matrix = Matrix()
    ) {
        render(
            Context2d.State(
                transform = transform,
                path = GraphicsPath().apply {
                    if (transform.getType() == Matrix.Type.IDENTITY) {
                        rect(x, y, width, height)
                    } else {
                        transformed(transform) {
                            rect(x, y, width, height)
                        }
                    }
                },
                fillStyle = BitmapPaint(
                    image,
                    transform = Matrix()
                        .scale(width / image.width.toDouble(), height / image.height.toDouble())
                        .translate(x, y)
                )
            ), fill = true)
    }

    inline fun drawImage(
        image: Bitmap,
        x: Number, y: Number, width: Number = image.width, height: Number = image.height,
        transform: Matrix = Matrix()
    ) = drawImage(image, x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(), transform)

    open fun dispose(): Unit {
        flush()
    }
}

open class DummyRenderer(override val width: Int, override val height: Int) : Renderer() {
    companion object : DummyRenderer(128, 128)
}

abstract class BufferedRenderer : Renderer() {
    abstract fun flushCommands()

    data class RenderCommand(
        val state: Context2d.State,
        val fill: Boolean,
        val font: Font? = null,
        val fontSize: Double = 0.0,
        val text: String? = null,
        val x: Double = 0.0,
        val y: Double = 0.0
    )
    protected val commands = arrayListOf<RenderCommand>()

    final override fun render(state: Context2d.State, fill: Boolean) {
        commands += RenderCommand(state.clone(), fill)
        if (!isBuffering()) flush()
    }

    //final override fun renderText(state: Context2d.State, font: Font, fontSize: Double, text: String, x: Double, y: Double, fill: Boolean) {
    //    commands += RenderCommand(state.clone(), fill, font, fontSize, text, x, y)
    //    if (!isBuffering()) flush()
    //}

    final override fun flush() = flushCommands()
}
