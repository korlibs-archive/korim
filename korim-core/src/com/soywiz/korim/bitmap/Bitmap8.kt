package com.soywiz.korim.bitmap

import com.soywiz.korim.color.RGBA
import com.soywiz.korio.util.UByteArray

class Bitmap8(
	width: Int,
	height: Int,
	val data: ByteArray = ByteArray(width * height),
	var palette: IntArray = IntArray(255)
) : Bitmap(width, height) {
	val datau = UByteArray(data)
	override val bpp = 8
	operator fun set(x: Int, y: Int, color: Int) = Unit.apply { datau[index(x, y)] = color }
	operator fun get(x: Int, y: Int): Int = datau[index(x, y)]
	override fun get32(x: Int, y: Int): Int = palette[get(x, y)]

	fun setRow(y: Int, row: UByteArray) {
		System.arraycopy(row.data, 0, data, index(0, y), width)
	}

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
