package com.soywiz.korim.bitmap

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korim.annotation.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlin.math.*
import kotlin.native.concurrent.*

abstract class Bitmap(
    val width: Int,
    val height: Int,
    val bpp: Int,
    var premultiplied: Boolean,
    val backingArray: Any?
) : Sizeable, Extra by Extra.Mixin() {
    @ThreadLocal
    protected val tempRgba: RgbaArray by lazy { RgbaArray(width * 2) }

    var contentVersion: Int = 0
	var texture: Any? = null

    protected var version = 0

    @Suppress("unused")
    val KorimInternalObject.version get() = this@Bitmap.version

	val stride: Int get() = (width * bpp) / 8
	val area: Int get() = width * height
	fun index(x: Int, y: Int) = y * width + x
	override val size: Size get() = Size(width, height)

    open fun lock() = Unit
    open fun unlock(rect: Rectangle? = null) = version++

    inline fun lock(rect: Rectangle? = null, block: () -> Unit) {
        lock()
        try {
            block()
        } finally {
            unlock(rect)
        }
    }

    open fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int = 0) {
        var n = offset
        for (y0 in 0 until height) for (x0 in 0 until width) out[n++] = getRgba(x0 + x, y0 + y)
    }
    open fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int = 0) {
        var n = offset
        for (y0 in 0 until height) for (x0 in 0 until width) setRgba(x0 + x, y0 + y, out[n++])
    }

    open fun setRgba(x: Int, y: Int, v: RGBA): Unit = TODO()
    open fun getRgba(x: Int, y: Int): RGBA = Colors.TRANSPARENT_BLACK

	open fun setInt(x: Int, y: Int, color: Int): Unit = Unit
	open fun getInt(x: Int, y: Int): Int = 0

	fun getRgbaClamped(x: Int, y: Int): RGBA = if (inBounds(x, y)) getRgba(x, y) else Colors.TRANSPARENT_BLACK

	fun getRgbaSampled(x: Double, y: Double): RGBA {
		if (x < 0.0 || x >= width.toDouble() || y < 0.0 || y >= height.toDouble()) return Colors.TRANSPARENT_BLACK
		val x0 = x.toIntFloor()
		val x1 = x.toIntCeil()
		val y0 = y.toIntFloor()
		val y1 = y.toIntCeil()
		val xratio = x % 1
		val yratio = y % 1
		val c00 = getRgbaClamped(x0, y0)
		val c10 = if (inBounds(x1, y0)) getRgbaClamped(x1, y0) else c00
		val c01 = if (inBounds(x1, y1)) getRgbaClamped(x0, y1) else c00
		val c11 = if (inBounds(x1, y1)) getRgbaClamped(x1, y1) else c01
		val c1 = RGBA.mixRgba(c00, c10, xratio)
		val c2 = RGBA.mixRgba(c01, c11, xratio)
		return RGBA.mixRgba(c1, c2, yratio)
	}

    fun getRgbaSampled(x: Double, y: Double, count: Int, row: RgbaArray) {
        for (n in 0 until count) {
            row[n] = getRgbaSampled(x + n, y)
        }
    }

    fun copy(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) {
        val src = this

        val srcX0 = src.clampWidth(srcX)
        val srcX1 = src.clampWidth(srcX + width)
        val srcY0 = src.clampHeight(srcY)
        val srcY1 = src.clampHeight(srcY + height)

        val dstX0 = dst.clampWidth(dstX)
        val dstX1 = dst.clampWidth(dstX + width)
        val dstY0 = dst.clampHeight(dstY)
        val dstY1 = dst.clampHeight(dstY + height)

        val srcX = srcX0
        val srcY = srcY0
        val dstX = dstX0
        val dstY = dstY0

        val width = min(srcX1 - srcX0, dstX1 - dstX0)
        val height = min(srcY1 - srcY0, dstY1 - dstY0)

        copyUnchecked(srcX, srcY, dst, dstX, dstY, width, height)
    }

	protected open fun copyUnchecked(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) {
		for (y in 0 until height) {
            readPixelsUnsafe(srcX, srcY + y, width, 1, tempRgba, 0)
            dst.writePixelsUnsafe(dstX, dstY + y, width, 1, tempRgba, 0)
		}
	}

	fun inBoundsX(x: Int) = (x >= 0) && (x < width)
	fun inBoundsY(y: Int) = (y >= 0) && (y < height)

	fun inBounds(x: Int, y: Int) = inBoundsX(x) && inBoundsY(y)

	fun clampX(x: Int) = x.clamp(0, width - 1)
	fun clampY(y: Int) = y.clamp(0, height - 1)

    fun clampWidth(x: Int) = x.clamp(0, width)
    fun clampHeight(y: Int) = y.clamp(0, height)

    fun flipY() = this.apply {
		for (y in 0 until height / 2) swapRows(y, height - y - 1)
	}
	fun flipX() = this.apply {
		for (x in 0 until width / 2) swapColumns(x, width - x - 1)
	}
	
	open fun swapRows(y0: Int, y1: Int) {
		for (x in 0 until width) {
			val c0 = getInt(x, y0)
			val c1 = getInt(x, y1)
			setInt(x, y0, c1)
			setInt(x, y1, c0)
		}
	}

	open fun swapColumns(x0: Int, x1: Int) {
		for (y in 0 until height) {
			val c0 = getInt(x0, y)
			val c1 = getInt(x1, y)
			setInt(x0, y, c1)
			setInt(x1, y, c0)
		}
	}

    inline fun forEach(sx: Int = 0, sy: Int = 0, width: Int = this.width - sx, height: Int = this.height - sy, callback: (n: Int, x: Int, y: Int) -> Unit) {
        for (y in sy until sy + height) {
            var n = index(sx, sy + y)
            for (x in sx until sx + width) {
                callback(n++, x, y)
            }
        }
    }

    open fun getContext2d(antialiasing: Boolean = true): Context2d =
		throw UnsupportedOperationException("Not implemented context2d on Bitmap, please use NativeImage or Bitmap32 instead")

	open fun createWithThisFormat(width: Int, height: Int): Bitmap = invalidOp("Unsupported createWithThisFormat ($this)")

	open fun toBMP32(): Bitmap32 = Bitmap32(width, height, premultiplied = premultiplied).also { out ->
        this.readPixelsUnsafe(0, 0, width, height, out.data, 0)
    }

    fun toBMP32IfRequired(): Bitmap32 = if (this is Bitmap32) this else this.toBMP32()

    fun contentEquals(other: Bitmap): Boolean {
        if (this.width != other.width) return false
        if (this.height != other.height) return false
        for (y in 0 until height) for (x in 0 until width) {
            if (this.getRgba(x, y) != other.getRgba(x, y)) return false
        }
        return true
    }

    open fun clone(): Bitmap {
        val out = createWithThisFormat(width, height)
        copyUnchecked(0, 0, out, 0, 0, width, height)
        return out
    }
}

fun <T : Bitmap> T.createWithThisFormatTyped(width: Int, height: Int): T = this.createWithThisFormat(width, height) as T

fun <T : Bitmap> T.extract(x: Int, y: Int, width: Int, height: Int): T {
	val out = this.createWithThisFormatTyped(width, height)
	this.copy(x, y, out, 0, 0, width, height)
	return out
}

inline fun <T : Bitmap> T.context2d(antialiased: Boolean = true, callback: Context2d.() -> Unit): T {
    lock {
        val ctx = getContext2d(antialiased)
        try {
            callback(ctx)
        } finally {
            ctx.dispose()
        }
    }
    return this
}

fun <T : Bitmap> T.checkMatchDimensions(other: T): T {
    check((this.width == other.width) && (this.height == other.height)) { "Bitmap doesn't have the same dimensions (${width}x${height}) != (${other.width}x${other.height})" }
    return other
}
