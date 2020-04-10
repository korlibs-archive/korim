package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

inline fun buildShape(width: Int = 256, height: Int = 256, builder: ShapeBuilder.() -> Unit): Shape = ShapeBuilder(width, height).apply(builder).buildShape()

class ShapeBuilder(width: Int, height: Int) : Context2d(Renderer.DUMMY), Context2d.Drawable {
    override val rendererWidth: Int = width
    override val rendererHeight: Int = height

    val shapes = arrayListOf<Shape>()

    override fun rendererRender(state: State, fill: Boolean) {
        if (state.path.isEmpty()) return

        if (fill) {
            shapes += FillShape(path = state.path?.clone(), clip = state.clip?.clone(), paint = state.fillStyle, transform = state.transform.clone())
        } else {
            shapes += PolylineShape(
                path = state.path.clone(),
                clip = state.clip?.clone(),
                paint = state.strokeStyle,
                transform = state.transform.clone(),
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

    override fun Renderer.rendererRenderSystemText(state: State, font: Font, text: String, x: Double, y: Double, fill: Boolean) {
        shapes += TextShape(
            text = text,
            x = x, y = y,
            font = font,
            clip = state.clip?.clone(),
            fill = if (fill) state.fillStyle else null,
            stroke = if (fill) null else state.strokeStyle,
            halign = state.horizontalAlign,
            valign = state.verticalAlign,
            transform = state.transform.clone()
        )
    }

    override fun rendererDrawImage(image: Bitmap, x: Double, y: Double, width: Double, height: Double, transform: Matrix) {
        rendererRender(State(
            transform = transform,
            path = GraphicsPath().apply { rect(x, y, width.toDouble(), height.toDouble()) },
            fillStyle = Context2d.BitmapPaint(image,
                transform = Matrix()
                    .scale(width.toDouble() / image.width.toDouble(), height.toDouble() / image.height.toDouble())
                    .translate(x, y)
            )
        ), fill = true)
    }

    override fun rendererDispose() {
    }

    override fun rendererBufferingStart(): Int {
        return 0
    }

    override fun rendererBufferingEnd() {
    }

    override fun rendererGetBounds(font: Font, text: String, out: TextMetrics) {
        super.rendererGetBounds(font, text, out)
    }

    fun clear() {
        state.clone()
        shapes.clear()
    }
    fun buildShape(): Shape = CompoundShape(shapes.toList())

    override fun draw(c: Context2d) {
        c.draw(buildShape())
    }
}
