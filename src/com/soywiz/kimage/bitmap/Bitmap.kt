package com.soywiz.kimage.bitmap

open class Bitmap(val width: Int, val height: Int) {
	val area: Int get() = width * height
	fun index(x: Int, y: Int) = y * width + x

	open fun get32(x: Int, y: Int): Int = 0

	fun inBounds(x: Int, y: Int) = x >= 0 && y >= 0 && x < width && y < height

	fun toBMP32(): Bitmap32 = when (this) {
		is Bitmap32 -> this
		is Bitmap8 -> {
			val out = Bitmap32(width, height)
			for (y in 0 until height) for (x in 0 until width) out[x, y] = this.get32(x, y)
			out
		}
		else -> throw IllegalArgumentException("Invalid Bitmap")
	}
}