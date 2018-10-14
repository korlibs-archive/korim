package com.soywiz.korim.bitmap

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.error.*
import com.soywiz.korma.geom.*

abstract class Bitmap(
	val width: Int,
	val height: Int,
	val bpp: Int,
	var premult: Boolean,
	val backingArray: Any?
) : Sizeable, Extra by Extra.Mixin() {
	var texture: Any? = null

	val stride: Int get() = (width * bpp) / 8
	val area: Int get() = width * height
	fun index(x: Int, y: Int) = y * width + x
	override val size: Size get() = Size(width, height)

	fun set32(x: Int, y: Int, v: RGBA): Unit = set32Int(x, y, v.rgba)
	fun get32(x: Int, y: Int): RGBA = RGBA(get32Int(x, y))

	open fun set32Int(x: Int, y: Int, v: Int): Unit = TODO()
	open fun get32Int(x: Int, y: Int): Int = 0

	open fun setInt(x: Int, y: Int, color: Int): Unit = Unit
	open fun getInt(x: Int, y: Int): Int = 0

	fun get32Clamped(x: Int, y: Int): RGBA = RGBA(get32ClampedInt(x, y))
	fun get32ClampedInt(x: Int, y: Int): Int = if (inBounds(x, y)) get32Int(x, y) else Colors.TRANSPARENT_BLACK.rgba

	// @TODO: super-slow, optimize this! and probably expose some API to read several sampled pixels at once, to reuse computations as much as possible

	fun get32Sampled(x: Double, y: Double): RGBA = RGBA(get32SampledInt(x, y))

	fun get32SampledInt(x: Double, y: Double): Int {
		if (x < 0.0 || x >= width.toDouble() || y < 0.0 || y >= height.toDouble()) return Colors.TRANSPARENT_BLACK.rgba
		val x0 = x.toIntFloor()
		val x1 = x.toIntCeil()
		val y0 = y.toIntFloor()
		val y1 = y.toIntCeil()
		val xratio = x % 1
		val yratio = y % 1
		val c00 = get32ClampedInt(x0, y0)
		val c10 = if (inBounds(x1, y0)) get32ClampedInt(x1, y0) else c00
		val c01 = if (inBounds(x1, y1)) get32ClampedInt(x0, y1) else c00
		val c11 = if (inBounds(x1, y1)) get32ClampedInt(x1, y1) else c01
		val c1 = RGBA.blendRGBAInt(c00, c10, xratio)
		val c2 = RGBA.blendRGBAInt(c01, c11, xratio)
		return RGBA.blendRGBAInt(c1, c2, yratio)
	}

	open fun copy(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) {
		for (y in 0 until height) {
			for (x in 0 until width) {
				dst.setInt(dstX + x, dstY, this.getInt(srcX + x, srcY))
			}
		}
	}

	fun inBoundsX(x: Int) = (x >= 0) && (x < width)
	fun inBoundsY(y: Int) = (y >= 0) && (y < height)

	fun inBounds(x: Int, y: Int) = inBoundsX(x) && inBoundsY(y)

	fun clampX(x: Int) = x.clamp(0, width - 1)
	fun clampY(y: Int) = y.clamp(0, height - 1)

	fun flipY() = this.apply {
		for (y in 0 until height / 2) swapRows(y, height - y - 1)
	}

	open fun swapRows(y0: Int, y1: Int) {
		for (x in 0 until width) {
			val c0 = getInt(x, y0)
			val c1 = getInt(x, y1)
			setInt(x, y0, c1)
			setInt(x, y1, c0)
		}
	}

	inline fun context2d(antialiased: Boolean = true, callback: Context2d.() -> Unit) {
		val ctx = getContext2d(antialiased)
		try {
			callback(ctx)
		} finally {
			ctx.dispose()
		}
	}

	open fun getContext2d(antialiasing: Boolean = true): Context2d =
		throw UnsupportedOperationException("Not implemented context2d on Bitmap, please use NativeImage instead")

	open fun createWithThisFormat(width: Int, height: Int): Bitmap = invalidOp("Unsupported createWithThisFormat ($this)")

	open fun toBMP32(): Bitmap32 = when (this) {
		is Bitmap32 -> this
		is NativeImage -> this.toBmp32()
		else -> Bitmap32(width, height, premult = premult).also { out ->
			val array = out.data.array
			var n = 0
			for (y in 0 until height) {
				for (x in 0 until width) {
					array[n++] = get32Int(x, y)
				}
			}
		}
	}
}

fun <T : Bitmap> T.createWithThisFormatTyped(width: Int, height: Int): T = this.createWithThisFormat(width, height) as T

fun <T : Bitmap> T.extract(x: Int, y: Int, width: Int, height: Int): T {
	val out = this.createWithThisFormatTyped(width, height)
	this.copy(x, y, out, 0, 0, width, height)
	return out
}
