package com.soywiz.korim.bitmap

import com.soywiz.korim.color.ColorFormat
import com.soywiz.korim.color.RGBA_4444

class Bitmap16(
	width: Int,
	height: Int,
	val data: ShortArray = ShortArray(width * height),
	val format: ColorFormat = RGBA_4444,
	premult: Boolean = false
) : Bitmap(width, height, 16, premult) {
	override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap16(width, height, format = format, premult = premult)

	operator override fun set(x: Int, y: Int, color: Int) = Unit.apply { data[index(x, y)] = color.toShort() }
	operator override fun get(x: Int, y: Int): Int = data[index(x, y)].toInt() and 0xFFFF

	override fun get32(x: Int, y: Int): Int = format.unpackToRGBA(data[index(x, y)].toInt())
	override fun set32(x: Int, y: Int, v: Int) = Unit.apply { data[index(x, y)] = format.packRGBA(v).toShort() }

	override fun toString(): String = "Bitmap16($width, $height, format=$format)"
}
