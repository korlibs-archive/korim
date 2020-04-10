package com.soywiz.korim.vector

import com.soywiz.kds.intArrayListOf
import com.soywiz.kmem.clamp
import com.soywiz.kmem.clamp01
import com.soywiz.kmem.toIntRound
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RGBAPremultiplied
import com.soywiz.korim.color.RgbaPremultipliedArray
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.shape.emitPoints
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.interpolation.interpolate
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
        state.path.emitPoints({
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

    abstract class BaseFiller {
        abstract fun fill(data: RgbaPremultipliedArray, x0: Int, x1: Int, y: Int)
    }

    object NoneFiller : BaseFiller() {
        override fun fill(data: RgbaPremultipliedArray, x0: Int, x1: Int, y: Int) = Unit
    }

    class ColorFiller : BaseFiller() {
        private var color: RGBAPremultiplied = Colors.RED.premultiplied

        fun set(fill: Context2d.Color, state: Context2d.State) = this.apply {
            this.color = fill.color.premultiplied
            //println("ColorFiller: $color")
        }

        override fun fill(data: RgbaPremultipliedArray, x0: Int, x1: Int, y: Int) {
            data.fill(color, x0, x1 + 1)
        }
    }

    class BitmapFiller : BaseFiller() {
        private var texture: Bitmap32 = Bitmaps.transparent.bmp
        private var transform: Matrix = Matrix()
        private var linear: Boolean = true
        private val stateTrans = Matrix()
        private val fillTrans = Matrix()
        private val compTrans = Matrix()

        fun set(fill: Context2d.BitmapPaint, state: Context2d.State) = this.apply {
            this.texture = fill.bmp32
            this.transform = fill.transform
            this.linear = fill.smooth
            state.transform.inverted(this.stateTrans)
            fill.transform.inverted(this.fillTrans)
            compTrans.apply {
                identity()
                multiply(this, stateTrans)
                multiply(this, fillTrans)
            }
        }

        fun lookupLinear(x: Double, y: Double): RGBA = texture.getRgbaSampled(x, y)
        fun lookupNearest(x: Double, y: Double): RGBA = texture[x.toInt(), y.toInt()]

        override fun fill(data: RgbaPremultipliedArray, x0: Int, x1: Int, y: Int) {
            /*
            val total = ((x1 - x0) + 1).toDouble()
            val tx0 = compTrans.transformX(x0, y)
            val ty0 = compTrans.transformY(x0, y)
            val tx1 = compTrans.transformX(x1, y)
            val ty1 = compTrans.transformY(x1, y)

            for (n in x0..x1) {
                val ratio = n / total
                val tx = ratio.interpolate(tx0, tx1)
                val ty = ratio.interpolate(ty0, ty1)
                val color = if (linear) lookupLinear(tx, ty) else lookupNearest(tx, ty)
                data[n] = color.premultiplied
            }
            */
            for (n in x0..x1) {
                val tx = compTrans.transformX(n, y)
                val ty = compTrans.transformY(n, y)
                val color = if (linear) lookupLinear(tx, ty) else lookupNearest(tx, ty)
                data[n] = color.premultiplied
            }
        }
    }

    class GradientFiller : BaseFiller() {
        private val NCOLORS = 256
        private val colors = RgbaPremultipliedArray(NCOLORS)
        private lateinit var fill: Context2d.Gradient
        private val stateTrans: Matrix = Matrix()
        private val fillTrans: Matrix = Matrix()
        private val compTrans: Matrix = Matrix()

        private fun stopN(n: Int): Int = (fill.stops[n] * NCOLORS).toInt()

        fun set(fill: Context2d.Gradient, state: Context2d.State) = this.apply {
            this.fill = fill
            state.transform.inverted(this.stateTrans)
            fill.transform.inverted(this.fillTrans)
            compTrans.apply {
                identity()
                multiply(this, stateTrans)
                multiply(this, fillTrans)
            }

            when (fill.numberOfStops) {
                0, 1 -> {
                    val color = if (fill.numberOfStops == 0) Colors.FUCHSIA else RGBA(fill.colors.first())
                    val pcolor = color.premultiplied
                    for (n in 0 until NCOLORS) colors[n] = pcolor
                }
                else -> {
                    for (n in 0 until stopN(0)) colors[n] = RGBA(fill.colors.first()).premultiplied
                    for (n in 0 until fill.numberOfStops - 1) {
                        val stop0 = stopN(n + 0)
                        val stop1 = stopN(n + 1)
                        val color0 = RGBA(fill.colors[n + 0])
                        val color1 = RGBA(fill.colors[n + 1])
                        for (s in stop0 until stop1) {
                            val ratio = (s - stop0).toDouble() / (stop1 - stop0).toDouble()
                            colors[s] = RGBA.interpolate(color0, color1, ratio).premultiplied
                        }
                    }
                    for (n in stopN(fill.numberOfStops - 1) until NCOLORS) colors.ints[n] = fill.colors.last()
                }
            }
        }

        private fun color(ratio: Double): RGBAPremultiplied {
            return colors[(ratio * (NCOLORS - 1)).toInt()]
        }

        private val p0 = Point()
        private val p1 = Point()
        private val tempMat = Matrix()

        // @TODO: Radial gradient
        override fun fill(data: RgbaPremultipliedArray, x0: Int, x1: Int, y: Int) {
            val p0 = this.p0.setTo(fill.x0, fill.y0)
            val p1 = this.p1.setTo(fill.x1, fill.y1)

            val mat = tempMat.copyFrom(compTrans).apply {
                translate(-p0.x, -p0.y)
                scale(1.0 / (p0.distanceTo(p1)).clamp(1.0, 16000.0))
                rotate(-Angle.between(p0, p1))
            }

            for (n in x0..x1) data[n] = color(mat.transformX(n.toDouble(), y.toDouble()).clamp01())
        }
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
                if (y >= 0) flush()
                ny = y
                reset()
            }
            if (ny0 != y0) {
                ny0 = y0
                subRowCount++
            }
            segments.add(i0, i1)
            if (i0 == i1) {
                put(i0, (x1 - x0).absoluteValue)
            } else {
                put(i0, computeAlpha(i0, x0))
                put(i1, computeAlpha(i1, x1))
                for (x in i0 + 1 until i1) {
                    put(x, 1.0)
                }
            }
            //alphaCount++
        }

        private fun computeAlpha(v: Int, p: Double): Double = if (v > p) (v - p) else 1.0 - (p - v)

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
                    val ualpha = this.alpha[x]
                    if (ualpha > 0) {
                        val alpha = ualpha * scale
                        val col = color[x]
                        val scaled = col.scaled(alpha)
                        //println("col=${col.hexString}:scaled=${scaled.hexString}:mixed=${mixed.hexString}:alpha=$alpha, ialpha=$ialpha, scale=$scale")
                        mix(rx, scaled)
                    }
                }
            }
        }
    }
}
