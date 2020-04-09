package com.soywiz.korim.vector.rasterizer

import com.soywiz.kmem.clamp
import com.soywiz.kmem.toIntCeil
import com.soywiz.kmem.toIntFloor
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korim.vector.Context2d
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.transformX
import com.soywiz.korma.geom.transformY
import com.soywiz.korma.interpolation.interpolate

abstract class BaseFiller(var bmp: Bitmap32) : Rasterizer.PaintSegment {
    override fun paint(a: Double, b: Double, y: Int, alpha: Double) {
        val yd = y.toDouble()
        val start0 = a.toIntFloor()
        val end0 = b.toIntFloor()
        val start = a.toIntCeil()
        val end = b.toIntFloor()
        if (start0 < start) {
            val x = bmp.index(start0, y).clamp(0, bmp.width - 1)
            fill(bmp.data, x, x, start0.toDouble(), start0.toDouble(), yd, 1.0 - (a - start0))
        }
        run {
            val x0 = bmp.index(start.clamp(0, bmp.width - 1), y)
            val x1 = bmp.index(end.clamp(0, bmp.width - 1), y)
            fill(bmp.data, x0, x1, start.toDouble(), end.toDouble(), yd, 1.0)
        }
    }

    protected fun put(data: RgbaArray, index: Int, color: RGBA, alpha: Double) {
        data[index] = RGBA.mixRgba(data[index], color, alpha)
    }

    abstract fun fill(data: RgbaArray, a: Int, b: Int, x0: Double, x1: Double, y: Double, alpha: Double)
}

class NoneFiller(bmp: Bitmap32) : BaseFiller(bmp) {
    fun set(fillStyle: Context2d.None, state: Context2d.State) = this.apply {
    }

    override fun fill(data: RgbaArray, a: Int, b: Int, x0: Double, x1: Double, y: Double, alpha: Double) {
    }
}

class GradientFiller(bmp: Bitmap32) : BaseFiller(bmp) {
    fun set(fillStyle: Context2d.Gradient, state: Context2d.State) = this.apply {
    }

    override fun fill(data: RgbaArray, a: Int, b: Int, x0: Double, x1: Double, y: Double, alpha: Double) {
    }
}

class ColorFiller(bmp: Bitmap32) : BaseFiller(bmp) {
    private var color: RGBA = Colors.RED

    fun set(style: Context2d.Color, state: Context2d.State) = this.apply {
        this.color = style.color
        println("ColorFiller: $color")
    }

    override fun fill(data: RgbaArray, a: Int, b: Int, x0: Double, x1: Double, y: Double, alpha: Double) {
        for (n in a..b) put(data, n, color, alpha)
    }
}

class BitmapFiller(bmp: Bitmap32) : BaseFiller(bmp) {
    private var texture: Bitmap32 = Bitmaps.transparent.bmp
    private var transform: Matrix = Matrix()
    private var linear: Boolean = true

    fun set(fillStyle: Context2d.BitmapPaint, state: Context2d.State) = this.apply {
        this.texture = fillStyle.bmp32
        this.transform = fillStyle.transform
        this.linear = fillStyle.smooth
    }

    fun lookupLinear(x: Double, y: Double): RGBA = texture.getRgbaSampled(x, y)
    fun lookupNearest(x: Double, y: Double): RGBA = texture[x.toInt(), y.toInt()]

    override fun fill(data: RgbaArray, a: Int, b: Int, x0: Double, x1: Double, y: Double, alpha: Double) {
        val tx0 = transform.transformX(x0, y)
        val tx1 = transform.transformX(x1, y)
        val ty0 = transform.transformY(x0, y)
        val ty1 = transform.transformY(x1, y)
        val total = ((b - a) + 1).toDouble()

        for (n in a..b) {
            val ratio = n / total
            val tx = ratio.interpolate(tx0, tx1)
            val ty = ratio.interpolate(ty0, ty1)
            val color = if (linear) lookupLinear(tx, ty) else lookupNearest(tx, ty)
            put(data, n, color, alpha)
        }
    }
}

/*
abstract class Filler<T : Context2d.Paint> {
    protected lateinit var fill: T
    protected lateinit var state: Context2d.State

    fun set(paint: T, state: Context2d.State) = this.apply {
        this.fill = paint
        this.state = state
        updated()
    }

    open fun updated() {
    }

    abstract fun fill(data: RgbaArray, offset: Int, x: Int, y: Int, count: Int)
}

class NoneFiller : Filler<Context2d.None>() {
    override fun fill(data: RgbaArray, offset: Int, x: Int, y: Int, count: Int) {
    }
}

class ColorFiller : Filler<Context2d.Color>() {
    override fun fill(data: RgbaArray, offset: Int, x: Int, y: Int, count: Int) {
        val c = fill.color.value
        for (n in 0 until count) {
            data.ints[offset + n] = c
        }
    }
}

class BitmapFiller(val antialiasing: Boolean) : Filler<Context2d.BitmapPaint>() {
    lateinit var stateTrans: Matrix
    lateinit var fillTrans: Matrix

    override fun updated() {
        stateTrans = state.transform.inverted()
        fillTrans = fill.transform.inverted()
    }

    override fun fill(data: RgbaArray, offset: Int, x: Int, y: Int, count: Int) {
        for (n in 0 until count) {
            // @TODO: Optimize. We can calculate start and end points and interpolate
            val bmpX = fillTrans.transformX(x + n, y)
            val bmpY = fillTrans.transformY(y + n, y)
            if (antialiasing) {
                data[offset + n] = fill.bitmap.getRgbaSampled(bmpX, bmpY)
            } else {
                data[offset + n] = fill.bitmap.getRgbaClamped(bmpX.toInt(), bmpY.toInt())
            }
        }
    }
}

class GradientFiller : Filler<Context2d.Gradient>() {
    val NCOLORS = 256
    val colors = RgbaArray(NCOLORS)

    fun stopN(n: Int): Int = (fill.stops[n] * NCOLORS).toInt()

    lateinit var stateTrans: Matrix
    lateinit var fillTrans: Matrix

    override fun updated() {
        stateTrans = state.transform.inverted()
        fillTrans = fill.transform.inverted()
        for (n in 0 until stopN(0)) colors[n] = RGBA(fill.colors.first())
        for (n in 0 until fill.numberOfStops - 1) {
            val stop0 = stopN(n + 0)
            val stop1 = stopN(n + 1)
            val color0 = RGBA(fill.colors[n + 0])
            val color1 = RGBA(fill.colors[n + 1])
            for (s in stop0 until stop1) {
                val ratio = (s - stop0).toDouble() / (stop1 - stop0).toDouble()
                colors[s] = RGBA.interpolate(color0, color1, ratio)
            }
        }
        for (n in stopN(fill.numberOfStops - 1) until NCOLORS) colors.ints[n] = fill.colors.last()
        //println(colors.map { RGBA.toHexString(it) })
    }

    override fun fill(data: RgbaArray, offset: Int, x: Int, y: Int, count: Int) {

        val p0 = Point(fill.x0, fill.y0)
        val p1 = Point(fill.x1, fill.y1)

        val mat = Matrix().apply {
            multiply(this, stateTrans)
            multiply(this, fillTrans)
            translate(-p0.x, -p0.y)
            scale(1.0 / (p0.distanceTo(p1)).clamp(1.0, 16000.0))
            rotate(-Angle.between(p0, p1))
        }

        for (n in 0 until count) {
            val ratio = mat.transformX((x + n).toDouble(), y.toDouble()).clamp01()
            data.ints[offset + n] = colors.ints[(ratio * (NCOLORS - 1)).toInt()]
        }
    }
}
 */
