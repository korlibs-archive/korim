package com.soywiz.korim.bitmap

import com.soywiz.kmem.*
import com.soywiz.korim.color.*

class Bitmap16(
    width: Int,
    height: Int,
    val data: ShortArray = ShortArray(width * height),
    val format: ColorFormat = RGBA_4444,
    premultiplied: Boolean = false
) : Bitmap(width, height, 16, premultiplied, data) {
	override fun createWithThisFormat(width: Int, height: Int): Bitmap =
		Bitmap16(width, height, format = format, premultiplied = premultiplied)

	operator fun set(x: Int, y: Int, color: Int) = setInt(x, y, color)
	operator fun get(x: Int, y: Int): Int = getInt(x, y)

	override fun setInt(x: Int, y: Int, color: Int) = Unit.apply { data[index(x, y)] = color.toShort() }
	override fun getInt(x: Int, y: Int): Int = data[index(x, y)].toInt() and 0xFFFF

	override fun setRgba(x: Int, y: Int, v: RGBA) = setInt(x, y, format.packRGBA(v))

	override fun getRgba(x: Int, y: Int): RGBA = format.unpackToRGBA(data[index(x, y)].toInt())

	override fun copy(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) {
        if (dst !is Bitmap16) return super.copy(srcX, srcY, dst, dstX, dstY, width, height)

        val src = this

		val srcArray = src.data
		var srcIndex = src.index(srcX, srcY)
		val srcAdd = src.width

		val dstArray = (dst as Bitmap16).data
		var dstIndex = dst.index(dstX, dstY)
		val dstAdd = dst.width

		for (y in 0 until height) {
			arraycopy(srcArray, srcIndex, dstArray, dstIndex, width)
			srcIndex += srcAdd
			dstIndex += dstAdd
		}
	}

	override fun toString(): String = "Bitmap16($width, $height, format=$format)"
}
