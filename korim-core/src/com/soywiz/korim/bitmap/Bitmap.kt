package com.soywiz.korim.bitmap

import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.util.clamp

abstract class Bitmap(
	val width: Int,
	val height: Int,
	val bpp: Int
) {
	val stride: Int get() = (width * bpp) / 8
	val area: Int get() = width * height
	fun index(x: Int, y: Int) = y * width + x

	open fun get32(x: Int, y: Int): Int = 0

	fun inBoundsX(x: Int) = (x >= 0) && (x < width)
	fun inBoundsY(y: Int) = (y >= 0) && (y < height)

	fun inBounds(x: Int, y: Int) = inBoundsX(x) && inBoundsY(y)

	fun clampX(x: Int) = x.clamp(0, width - 1)
	fun clampY(y: Int) = y.clamp(0, height - 1)

	fun flipY() = this.apply {
		for (y in 0 until height / 2) swapRows(y, height - y - 1)
	}

	abstract fun swapRows(y0: Int, y1: Int)

	open fun getContext2d(): Context2d = throw UnsupportedOperationException()

	fun toBMP32(): Bitmap32 = when (this) {
		is Bitmap32 -> this
		is NativeImage -> this.toBmp32()
		else -> {
			val out = Bitmap32(width, height)
			for (y in 0 until height) for (x in 0 until width) out[x, y] = this.get32(x, y)
			out
		}
	}
}