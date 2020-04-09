package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBAPremultiplied
import com.soywiz.korim.color.RgbaPremultipliedArray
import com.soywiz.korim.vector.rasterizer.*
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.float
import com.soywiz.korma.geom.vector.VectorPath
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
	val colorFiller = ColorFiller()
	val gradientFiller = GradientFiller()
	val bitmapFiller = BitmapFiller()

    // @TODO: Optimize. We could handle a few more chunks
    inner class ScanlineWriter {
        var filler: BaseFiller = NoneFiller
        var ny = -1
        val size = bmp.width
        var alphaCount = 0
        val alpha = IntArray(size)
        val color = RgbaPremultipliedArray(size)
        var xmin = size - 1
        var xmax = 0
        fun reset() {
            alpha.fill(0)
            alphaCount = 0
            xmin = size
            xmax = 0
        }
        fun select(a: Int, b: Int, y: Int) {
            val x0 = a.coerceIn(0, bmp.width - 1)
            val x1 = b.coerceIn(0, bmp.width - 1)

            if (ny != y) {
                if (y >= 0) {
                    flush()
                }
                ny = y
                reset()
            }
            xmin = min(xmin, x0)
            xmax = max(xmax, x1)
            for (x in x0..x1) {
                val calpha = ++alpha[x]
                alphaCount = max(alphaCount, calpha)
            }
        }
        fun flush() {
            if (ny >= 0 && ny < bmp.height) {
                if (bmp.premultiplied) {
                    val data = bmp.dataPremult
                    render0 { index, color ->
                        val mixed = RGBAPremultiplied.mix(data[index], color)
                        data[index] = mixed
                    }
                } else {
                    val data = bmp.data
                    render0 { index, color ->
                        data[index] = RGBAPremultiplied.mix(data[index].premultiplied, color).depremultiplied
                        //data[index] = RGBA.mix(data[index], color.depremultiplied)
                    }
                }
            }
        }

        // PERFORMANCE: This is inline so we have two specialized versions without ifs on the inner loop
        private inline fun render0(mix: (index: Int, color: RGBAPremultiplied) -> Unit) {
            filler.fill(color, xmin, xmax, ny)
            val row = bmp.index(0, ny)
            val scale = 1.0 / alphaCount
            // @TODO: Critical. Use SIMD
            for (x in xmin..xmax) {
                val rx = row + x
                val ialpha = this.alpha[x]
                if (ialpha > 0) {
                    val alpha = ialpha * scale
                    val col = color[x]
                    val scaled = col.scaled(alpha)
                    //println("col=${col.hexString}:scaled=${scaled.hexString}:mixed=${mixed.hexString}:alpha=$alpha, ialpha=$ialpha, scale=$scale")
                    mix(rx, scaled)
                }
            }
        }
    }

    val scanlineWriter = ScanlineWriter()


    override fun render(state: Context2d.State, fill: Boolean) {
		//println("RENDER")
		val style = if (fill) state.fillStyle else state.strokeStyle
		val filler = when (style) {
			is Context2d.None -> NoneFiller
			is Context2d.Color -> colorFiller.set(style, state)
			is Context2d.Gradient -> gradientFiller.set(style, state)
			is Context2d.BitmapPaint -> bitmapFiller.set(style, state)
			else -> TODO()
		}

        rasterizer.debug = debug
        rasterizer.reset()
        state.path.emitPoints2({
            if (it) rasterizer.close()
        }, { x, y ->
            rasterizer.add(x, y)
        })
        rasterizer.strokeWidth = state.lineWidth
        rasterizer.quality = if (antialiasing) 8 else 2
        scanlineWriter.filler = filler
        scanlineWriter.reset()
        rasterizer.rasterize(bounds, fill) { x0, x1, y ->
            scanlineWriter.select(x0.roundToInt(), x1.roundToInt(), y.toInt())
        }
        scanlineWriter.flush()
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
