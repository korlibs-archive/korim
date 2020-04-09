package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.vector.rasterizer.*
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.float
import com.soywiz.korma.geom.vector.VectorPath

// References:
// - https://github.com/memononen/nanosvg/blob/master/src/nanosvgrast.h
// - https://www.geeksforgeeks.org/scan-line-polygon-filling-using-opengl-c/
// - https://hackernoon.com/computer-graphics-scan-line-polygon-fill-algorithm-3cb47283df6
// - https://nothings.org/gamedev/rasterize/
// - https://www.mathematik.uni-marburg.de/~thormae/lectures/graphics1/code_v2/RasterPoly/index.html
class Bitmap32Context2d(val bmp: Bitmap32, val antialiasing: Boolean) : Context2d.Renderer() {
	override val width: Int get() = bmp.width
	override val height: Int get() = bmp.height

    val bounds = bmp.bounds.float
    val rasterizer = Rasterizer()
	val colorFiller = ColorFiller(bmp)
	val gradientFiller = GradientFiller(bmp)
	val bitmapFiller = BitmapFiller(bmp)
	val noneFiller = NoneFiller(bmp)

	override fun render(state: Context2d.State, fill: Boolean) {
		//println("RENDER")
		val style = if (fill) state.fillStyle else state.strokeStyle
		val filler = when (style) {
			is Context2d.None -> noneFiller.set(style, state)
			is Context2d.Color -> colorFiller.set(style, state)
			is Context2d.Gradient -> gradientFiller.set(style, state)
			is Context2d.BitmapPaint -> bitmapFiller.set(style, state)
			else -> TODO()
		}

        rasterizer.reset()
        state.path.emitPoints2({
            if (it) rasterizer.close()
        }, { x, y ->
            rasterizer.add(x, y)
        })
        if (fill) {
            rasterizer.rasterizeFill(bounds, filler)
        } else {
            rasterizer.rasterizeStroke(bounds, state.lineWidth, filler)
        }
	}

    private inline fun VectorPath.emitPoints2(flush: (close: Boolean) -> Unit, emit: (x: Double, y: Double) -> Unit, curveSteps: Int = 20) {
        var lx = 0.0
        var ly = 0.0
        flush(false)
        this.visitCmds(
            moveTo = { x, y ->
                //kotlin.io.println("moveTo")
                emit(x, y)
                lx = x
                ly = y
            },
            lineTo = { x, y ->
                //kotlin.io.println("lineTo")
                emit(x, y)
                lx = x
                ly = y
            },
            quadTo = { x0, y0, x1, y1 ->
                //kotlin.io.println("quadTo")
                val dt = 1.0 / curveSteps
                for (n in 1 until curveSteps) {
                    Bezier.quadCalc(lx, ly, x0, y0, x1, y1, n * dt, emit)
                }
                lx = x1
                ly = y1
            },
            cubicTo = { x0, y0, x1, y1, x2, y2 ->
                //kotlin.io.println("cubicTo")
                val dt = 1.0 / curveSteps
                for (n in 1 until curveSteps) {
                    Bezier.cubicCalc(lx, ly, x0, y0, x1, y1, x2, y2, n * dt, emit)
                }
                lx = x2
                ly = y2
            },
            close = {
                flush(true)
            }
        )
        flush(false)
    }
}
