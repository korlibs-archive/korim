package com.soywiz.korim.vector

import com.soywiz.kds.intArrayListOf
import com.soywiz.kmem.toIntRound
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.filler.*
import com.soywiz.korim.vector.paint.BitmapPaint
import com.soywiz.korim.vector.paint.ColorPaint
import com.soywiz.korim.vector.paint.GradientPaint
import com.soywiz.korim.vector.paint.NonePaint
import com.soywiz.korim.vector.rasterizer.Rasterizer
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.shape.emitPoints
import com.soywiz.korma.geom.vector.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

// References:
// - https://github.com/memononen/nanosvg/blob/master/src/nanosvgrast.h
// - https://www.geeksforgeeks.org/scan-line-polygon-filling-using-opengl-c/
// - https://hackernoon.com/computer-graphics-scan-line-polygon-fill-algorithm-3cb47283df6
// - https://nothings.org/gamedev/rasterize/
// - https://www.mathematik.uni-marburg.de/~thormae/lectures/graphics1/code_v2/RasterPoly/index.html
class Bitmap32Context2d(val bmp: Bitmap32, val antialiasing: Boolean) : com.soywiz.korim.vector.renderer.Renderer() {
	override val width: Int get() = bmp.width
	override val height: Int get() = bmp.height

    val bounds = bmp.bounds.float
    val rasterizer = Rasterizer()
	val colorFiller = ColorFiller()
	val gradientFiller = GradientFiller()
	val bitmapFiller = BitmapFiller()
    val scanlineWriter = ScanlineWriter()

    override fun render(state: Context2d.State, fill: Boolean) {
		//println("RENDER")
		val style = if (fill) state.fillStyle else state.strokeStyle
		val filler = when (style) {
			is NonePaint -> NoneFiller
			is ColorPaint -> colorFiller.set(style, state)
			is GradientPaint -> gradientFiller.set(style, state)
			is BitmapPaint -> bitmapFiller.set(style, state)
			else -> TODO()
		}

        rasterizer.debug = debug
        state.path.emitPoints2({
        //state.path.emitPoints({
            if (it) rasterizer.close()
        }, { x, y ->
            rasterizer.add(x, y)
        })
        if (rasterizer.size > 0) {
            rasterizer.strokeWidth = state.lineWidth
            rasterizer.quality = if (antialiasing) 4 else 1
            scanlineWriter.filler = filler
            scanlineWriter.reset()
            rasterizer.rasterize(bounds, fill) { x0, x1, y ->
                scanlineWriter.select(x0, x1, y)
            }
            scanlineWriter.flush()
            rasterizer.reset()
        }
    }

    @PublishedApi
    internal inline fun approximateCurve(
        curveSteps: Int,
        crossinline compute: (ratio: Double, get: (x: Double, y: Double) -> Unit) -> Unit,
        crossinline emit: (x: Double, y: Double) -> Unit
    ) {
        val rcurveSteps = max(curveSteps, 20)
        val dt = 1.0 / rcurveSteps
        var lastX = 0.0
        var lastY = 0.0
        var prevX = 0.0
        var prevY = 0.0
        var emittedCount = 0
        compute(0.0) { x, y ->
            lastX = x
            lastY = y
        }
        for (n in 1 until rcurveSteps) {
            val ratio = n * dt
            //println("ratio: $ratio")
            compute(ratio) { x, y ->
                //if (emittedCount == 0) {
                run {
                    emit(x, y)
                    emittedCount++
                    lastX = prevX
                    lastY = prevY
                }

                prevX = x
                prevY = y
            }
        }
        //println("curveSteps: $rcurveSteps, emittedCount=$emittedCount")
    }

    inline fun VectorPath.emitPoints2(crossinline flush: (close: Boolean) -> Unit, crossinline emit: (x: Double, y: Double) -> Unit) {
        var lx = 0.0
        var ly = 0.0
        flush(false)
        this.visitCmds(
            moveTo = { x, y -> emit(x, y).also { lx = x }.also { ly = y } },
            lineTo = { x, y -> emit(x, y).also { lx = x }.also { ly = y } },
            quadTo = { x0, y0, x1, y1 ->
                val sum = Point.distance(lx, ly, x0, y0) + Point.distance(x0, y0, x1, y1)
                approximateCurve(sum.toInt(), { ratio, get -> Bezier.quadCalc(lx, ly, x0, y0, x1, y1, ratio) { x, y -> get(x, y) } }) { x, y -> emit(x, y) }
                run { lx = x1 }.also { ly = y1 }
            },
            cubicTo = { x0, y0, x1, y1, x2, y2 ->
                val sum = Point.distance(lx, ly, x0, y0) + Point.distance(x0, y0, x1, y1) + Point.distance(x1, y1, x2, y2)
                approximateCurve(sum.toInt(), { ratio, get -> Bezier.cubicCalc(lx, ly, x0, y0, x1, y1, x2, y2, ratio) { x, y -> get(x, y) }}) { x, y -> emit(x, y) }
                run { lx = x2 }.also { ly = y2 }

            },
            close = { flush(true) }
        )
        flush(false)
    }

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
        val alpha = FloatArray(size)
        val hitbits = IntArray(size)
        val color = RgbaPremultipliedArray(size)
        val segments = SegmentHandler()
        var subRowCount = 0
        fun reset() {
            alpha.fill(0f)
            hitbits.fill(0)
            subRowCount = 0
            segments.reset()
        }
        fun select(x0: Double, x1: Double, y0: Double) {
            if (width1 < 1) return
            val x0 = x0.coerceIn(0.0, width1.toDouble())
            val x1 = x1.coerceIn(0.0, width1.toDouble())
            val a = x0.toIntRound()
            val b = x1.toIntRound()
            val y = y0.toInt()
            val i0 = a.coerceIn(0, width1)
            val i1 = b.coerceIn(0, width1)

            if (ny != y) {
                if (y >= 0) flush()
                ny = y
                reset()
            }
            if (ny0 != y0) {
                ny0 = y0
                subRowCount++
            }
            segments.add(i0, i1)
            //println("ROW[$y0]: $x0,$x1")
            if (i0 == i1) {
                put(i0, (x1 - x0).absoluteValue.toFloat())
            } else {
                put(i0, computeAlpha(i0, x0.toFloat(), true))
                put(i1, computeAlpha(i1, x1.toFloat(), false))
                //println("i1=$i1, x1=$x1")
                for (x in i0 + 1 until i1) {
                    put(x, 1f)
                }
            }
            //alphaCount++
        }

        private fun computeAlpha(v: Int, p: Float, left: Boolean): Float = when {
            v.toFloat() == p -> 1f
            v > p != left -> (v - p).absoluteValue
            else -> 1f - (v - p).absoluteValue
        }

        fun put(x: Int, ratio: Float) {
            val mask = 1 shl subRowCount
            if ((hitbits[x] and mask) == 0) {
                hitbits[x] = hitbits[x] or mask
                alpha[x] += ratio
            }
        }

        fun flush() {
            if (ny !in 0 until bmp.height) return
            val scale = 1f / subRowCount
            segments.forEachFast { xmin, xmax ->
                val x = xmin
                val count = xmax - xmin + 1
                filler.fill(color, 0, xmin, xmax, ny)
                for (n in xmin..xmax) alpha[n] *= scale
                scale(color, xmin, alpha, xmin, count)
                if (bmp.premultiplied) {
                    mix(bmp.dataPremult, bmp.index(0, ny) + x, color, x, count)
                } else {
                    mix(bmp.data, bmp.index(0, ny) + x, color, x, count)
                }
            }
        }
    }
}
