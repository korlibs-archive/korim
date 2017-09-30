package com.soywiz.korim.bitmap

import com.soywiz.korim.color.RGBA
import com.soywiz.korio.typedarray.copyRangeTo
import com.soywiz.korio.util.UByteArray
import com.soywiz.korio.util.insert
import kotlin.math.max

abstract class BitmapIndexed(
	bpp: Int,
	width: Int, height: Int,
	var data: ByteArray = ByteArray(width * height / (8 / bpp)),
	var palette: IntArray = IntArray(1 shl bpp)
) : Bitmap(width, height, bpp, false) {
	init {
		if (data.size < width * height / (8 / bpp)) throw RuntimeException("Bitmap data is too short: width=$width, height=$height, data=ByteArray(${data.size}), area=${width * height}")
	}

	override fun toString() = "BitmapIndexed(bpp=$bpp, width=$width, height=$height, clut=${palette.size})"

	protected val temp = ByteArray(max(width, height))

	val datau = UByteArray(data)
	val n8_dbpp: Int = 8 / bpp
	val mask = ((1 shl bpp) - 1)
	operator override fun get(x: Int, y: Int): Int = (datau[index_d(x, y)] ushr (bpp * index_m(x, y))) and mask
	operator override fun set(x: Int, y: Int, color: Int): Unit {
		val i = index_d(x, y)
		datau[i] = datau[i].insert(color, bpp * index_m(x, y), bpp)
	}

	override fun get32(x: Int, y: Int): Int = palette[this[x, y]]
	fun index_d(x: Int, y: Int) = index(x, y) / n8_dbpp
	fun index_m(x: Int, y: Int) = index(x, y) % n8_dbpp

	fun setRow(y: Int, row: UByteArray) {
		row.data.copyRangeTo(0, data, index(0, y), stride)
	}

	fun setRow(y: Int, row: ByteArray) {
		row.copyRangeTo(0, data, index(0, y), stride)
	}

	fun setWhitescalePalette() = this.apply {
		for (n in 0 until palette.size) {
			val col = ((n.toFloat() / palette.size.toFloat()) * 255).toInt()
			palette[n] = RGBA(col, col, col, 0xFF)
		}
		return this
	}

	override fun swapRows(y0: Int, y1: Int) {
		val s0 = index_d(0, y0)
		val s1 = index_d(0, y1)
		data.copyRangeTo(s0, temp, 0, stride)
		data.copyRangeTo(s1, data, s0, stride)
		temp.copyRangeTo(0, data, s1, stride)
	}

	fun toLines(palette: String): List<String> {
		return (0 until height).map { y -> (0 until height).map { x -> palette[this[x, y]] }.joinToString("") }
	}
}