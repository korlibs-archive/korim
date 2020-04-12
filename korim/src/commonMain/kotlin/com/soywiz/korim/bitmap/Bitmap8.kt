package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*

class Bitmap8(
	width: Int,
	height: Int,
	data: ByteArray = ByteArray(width * height),
	palette: RgbaArray = RgbaArray(255)
) : BitmapIndexed(8, width, height, data, palette) {
	override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap8(width, height)

	override fun setInt(x: Int, y: Int, color: Int) = Unit.apply { datau[index(x, y)] = color }
	override fun getInt(x: Int, y: Int): Int = datau[index(x, y)]
	override fun getRgba(x: Int, y: Int): RGBA = palette[get(x, y)]

    override fun clone() = Bitmap8(width, height, data.copyOf(), RgbaArray(palette.ints.copyOf()))

	override fun toString(): String = "Bitmap8($width, $height, palette=${palette.size})"
}
