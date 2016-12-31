package com.soywiz.korim.bitmap

import com.soywiz.korim.color.RGBA

class Bitmap8(
	width: Int,
	height: Int,
	val data: ByteArray = ByteArray(width * height),
	var palette: IntArray = IntArray(255)
) : Bitmap(width, height) {
	operator fun set(x: Int, y: Int, color: Int) = Unit.apply { data[index(x, y)] = color.toByte() }
	operator fun get(x: Int, y: Int): Int = data[index(x, y)].toInt() and 0xFF
	override fun get32(x: Int, y: Int): Int = palette[get(x, y)]

	fun setRow(y: Int, row: ByteArray) {
		System.arraycopy(row, 0, data, index(0, y), width)
	}

	fun setWhitescalePalette(): Bitmap8 {
		for (n in 0 until palette.size) {
			val col = ((n.toFloat() / palette.size.toFloat()) * 255).toInt()
			palette[n] = RGBA(col, col, col, 0xFF)
		}
		return this
	}

	override fun toString(): String = "Bitmap8($width, $height, palette=${palette.size})"
}
