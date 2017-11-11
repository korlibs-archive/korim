package com.soywiz.korim.bitmap

import com.soywiz.kds.Extra
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.clamp
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.Sizeable

abstract class Bitmap(
	val width: Int,
	val height: Int,
	val bpp: Int,
	var premult: Boolean
) : Sizeable, Extra by Extra.Mixin() {
	val stride: Int get() = (width * bpp) / 8
	val area: Int get() = width * height
	fun index(x: Int, y: Int) = y * width + x
	override val size: Size get() = Size(width, height)

	open fun set32(x: Int, y: Int, v: Int): Unit = TODO()
	open fun get32(x: Int, y: Int): Int = 0
	open operator fun set(x: Int, y: Int, color: Int): Unit = Unit
	open operator fun get(x: Int, y: Int) = 0

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
			val c0 = get(x, y0)
			val c1 = get(x, y1)
			set(x, y0, c1)
			set(x, y1, c0)
		}
	}

	open fun getContext2d(antialiasing: Boolean = true): Context2d = throw UnsupportedOperationException("Not implemented context2d on Bitmap, please use NativeImage instead")

	open fun createWithThisFormat(width: Int, height: Int): Bitmap = invalidOp("Unsupported createWithThisFormat")

	fun toBMP32(): Bitmap32 = when (this) {
		is Bitmap32 -> this
		is NativeImage -> this.toBmp32()
		else -> {
			val out = Bitmap32(width, height, 0, premult = premult)
			for (y in 0 until height) for (x in 0 until width) out[x, y] = this.get32(x, y)
			out
		}
	}
}

fun <T : Bitmap> T.createWithThisFormatTyped(width: Int, height: Int): T = this.createWithThisFormat(width, height) as T