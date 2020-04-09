package com.soywiz.korim.vector.rasterizer

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.Context2d
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.transformX
import com.soywiz.korma.geom.transformY
import com.soywiz.korma.interpolation.interpolate

// @TODO: We should optimize this

abstract class BaseFiller {
    abstract fun fill(data: RgbaPremultipliedArray, x0: Int, x1: Int, y: Int)
}

object NoneFiller : BaseFiller() {
    override fun fill(data: RgbaPremultipliedArray, x0: Int, x1: Int, y: Int) = Unit
}

class GradientFiller() : BaseFiller() {
    fun set(fillStyle: Context2d.Gradient, state: Context2d.State) = this.apply {
    }

    override fun fill(data: RgbaPremultipliedArray, x0: Int, x1: Int, y: Int) {
        println("Not implemented GradientFiller")
    }
}

class ColorFiller() : BaseFiller() {
    private var color: RGBAPremultiplied = Colors.RED.premultiplied

    fun set(style: Context2d.Color, state: Context2d.State) = this.apply {
        this.color = style.color.premultiplied
        //println("ColorFiller: $color")
    }

    override fun fill(data: RgbaPremultipliedArray, x0: Int, x1: Int, y: Int) {
        data.fill(color, x0, x1)
    }
}

class BitmapFiller() : BaseFiller() {
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

    override fun fill(data: RgbaPremultipliedArray, x0: Int, x1: Int, y: Int) {
        val tx0 = transform.transformX(x0, y)
        val tx1 = transform.transformX(x1, y)
        val ty0 = transform.transformY(x0, y)
        val ty1 = transform.transformY(x1, y)
        val total = ((x1 - x0) + 1).toDouble()

        for (n in x0..x1) {
            val ratio = n / total
            val tx = ratio.interpolate(tx0, tx1)
            val ty = ratio.interpolate(ty0, ty1)
            val color = if (linear) lookupLinear(tx, ty) else lookupNearest(tx, ty)
            data[n] = color.premultiplied
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
