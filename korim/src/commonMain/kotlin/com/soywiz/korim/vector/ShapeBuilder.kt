package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*

inline fun buildShape(builder: ShapeBuilder.() -> Unit): Shape = ShapeBuilder().apply(builder).buildShape()

class ShapeBuilder : Context2d(Renderer.DUMMY), Context2d.Drawable {
    override val rendererWidth: Int get() = super.rendererWidth
    override val rendererHeight: Int get() = super.rendererHeight

    val shapes = arrayListOf<Shape>()

    override fun rendererRender(state: State, fill: Boolean) {
        if (fill) {
            shapes += FillShape(path = state.path, clip = state.clip, paint = state.fillStyle, transform = state.transform)
        } else {
            shapes += PolylineShape(
                path = state.path,
                clip = state.clip,
                paint = state.strokeStyle,
                transform = state.transform,
                thickness = state.lineWidth,
                pixelHinting = true,
                scaleMode = state.lineScaleMode,
                startCaps = state.startLineCap,
                endCaps = state.endLineCap,
                lineJoin = state.lineJoin,
                miterLimit = state.miterLimit
            )
        }
        super.rendererRender(state, fill)
    }

    override fun rendererRenderText(state: State, font: Font, text: String, x: Double, y: Double, fill: Boolean) {
        super.rendererRenderText(state, font, text, x, y, fill)
    }

    override fun rendererDrawImage(image: Bitmap, x: Int, y: Int, width: Int, height: Int, transform: Matrix) {
        super.rendererDrawImage(image, x, y, width, height, transform)
    }

    override fun rendererDispose() {
        super.rendererDispose()
    }

    override fun rendererBufferingStart(): Int {
        return super.rendererBufferingStart()
    }

    override fun rendererBufferingEnd() {
        super.rendererBufferingEnd()
    }

    override fun rendererGetBounds(font: Font, text: String, out: TextMetrics) {
        super.rendererGetBounds(font, text, out)
    }

    fun clear() {
        shapes.clear()
    }
    fun buildShape(): Shape = CompoundShape(shapes.toList())

    override fun draw(c: Context2d) {
        c.draw(buildShape())
    }
}
