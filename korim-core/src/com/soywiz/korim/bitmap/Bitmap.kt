package com.soywiz.korim.bitmap

import com.soywiz.korio.util.clamp

open class Bitmap(val width: Int, val height: Int) {
	val area: Int get() = width * height
	fun index(x: Int, y: Int) = y * width + x

	open fun get32(x: Int, y: Int): Int = 0

	fun inBoundsX(x: Int) = (x >= 0) && (x < width)
	fun inBoundsY(y: Int) = (y >= 0) && (y < height)

	fun inBounds(x: Int, y: Int) = inBoundsX(x) && inBoundsY(y)

	fun clampX(x: Int) = x.clamp(0, width - 1)
	fun clampY(y: Int) = y.clamp(0, height - 1)

	fun toBMP32(): Bitmap32 = when (this) {
		is Bitmap32 -> this
		is Bitmap8 -> {
			val out = Bitmap32(width, height)
			for (y in 0 until height) for (x in 0 until width) out[x, y] = this.get32(x, y)
			out
		}
		is NativeImage -> this.toBmp32()
		else -> throw IllegalArgumentException("Invalid Bitmap")
	}
}