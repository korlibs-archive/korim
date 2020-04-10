package com.soywiz.korim.vector

import com.soywiz.kds.intArrayListOf
import com.soywiz.kmem.toIntRound
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBAPremultiplied
import com.soywiz.korim.color.RgbaPremultipliedArray
import com.soywiz.korim.vector.rasterizer.*
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.float
import com.soywiz.korma.geom.vector.VectorPath
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

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

    class SegmentHandler {
        val xmin = intArrayListOf()
        val xmax = intArrayListOf()
        val size get() = xmin.size

        init {
            reset()
        }

        fun reset() {
            xmin.clear()
            xmax.clear()
        }

        private fun overlaps(a0: Int, a1: Int, b0: Int, b1: Int): Boolean {
            val min = min(a0, a0)
            val max = max(a1, a1)
            val maxMinor = max(a0, b0)
            val minMajor = min(a1, b1)
            return (maxMinor in min..max) || (minMajor in min..max)
        }

        fun add(x0: Int, x1: Int) {
            // @TODO: Maybe we can optimize this if we keep segments in order
            for (n in 0 until size) {
                val xmin = this.xmin[n]
                val xmax = this.xmax[n]
                if (overlaps(xmin, xmax, x0, x1)) {
                    this.xmin[n] = min(x0, xmin)
                    this.xmax[n] = max(x1, xmax)
                    return
                }
            }
            // Only works if done from left to right
            //if (size > 0 && overlaps(xmin[size - 1], xmax[size - 1], x0, x1)) {
            //    xmin[size - 1] = min(x0, xmin[size - 1])
            //    xmax[size - 1] = max(x0, xmax[size - 1])
            //} else {
            xmin.add(x0)
            xmax.add(x1)
            //}
        }

        inline fun forEachFast(block: (x0: Int, x1: Int) -> Unit) {
            for (n in 0 until size) {
                block(xmin[n], xmax[n])
            }
        }
    }

    inner class ScanlineWriter {
        var filler: BaseFiller = NoneFiller
        var ny0 = -1.0
        var ny = -1
        val size = bmp.width
        val width1 = bmp.width - 1
        val alpha = DoubleArray(size)
        val hitbits = IntArray(size)
        val color = RgbaPremultipliedArray(size)
        val segments = SegmentHandler()
        var subRowCount = 0
        fun reset() {
            alpha.fill(0.0)
            hitbits.fill(0)
            subRowCount = 0
            segments.reset()
        }
        fun select(x0: Double, x1: Double, y0: Double) {
            val x0 = x0.coerceIn(0.0, width1.toDouble())
            val x1 = x1.coerceIn(0.0, width1.toDouble())
            val a = x0.toIntRound()
            val b = x1.toIntRound()
            val y = y0.toInt()
            val i0 = a.coerceIn(0, width1)
            val i1 = b.coerceIn(0, width1)

            if (ny != y) {
                if (y >= 0) {
                    flush()
                }
                ny = y
                reset()
            }
            if (ny0 != y0) {
                ny0 = y0
                subRowCount++
            }
            segments.add(i0, i1)
            if (i0 == i1) {
                val pixelSize = (x1 - x0).absoluteValue
                put(i0, pixelSize)
            } else {
                val first = (x0 - i0).absoluteValue
                val last = (x1 - i1).absoluteValue
                put(i0, first)
                put(i1, last)
                for (x in i0 + 1 until i1) {
                    put(x, 1.0)
                }
            }
            //alphaCount++
        }

        fun put(x: Int, ratio: Double) {
            val mask = 1 shl subRowCount
            if ((hitbits[x] and mask) == 0) {
                hitbits[x] = hitbits[x] or mask
                alpha[x] += ratio
            }
        }

        // @TODO: We should try to use SIMD if possible
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
        @OptIn(ExperimentalStdlibApi::class)
        private inline fun render0(mix: (index: Int, color: RGBAPremultiplied) -> Unit) {
            val row = bmp.index(0, ny)
            val scale = 1.0 / subRowCount
            segments.forEachFast { xmin, xmax ->
                filler.fill(color, xmin, xmax, ny)
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
            scanlineWriter.select(x0, x1, y)
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
