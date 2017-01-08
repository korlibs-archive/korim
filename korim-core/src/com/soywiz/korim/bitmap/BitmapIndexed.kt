package com.soywiz.korim.bitmap

import com.soywiz.korim.color.RGBA
import com.soywiz.korio.util.UByteArray
import com.soywiz.korio.util.insert

abstract class BitmapIndexed(
	bpp: Int,
	width: Int, height: Int,
	var data: ByteArray = ByteArray(width * height / (8 / bpp)),
	var palette: IntArray = IntArray(1 shl bpp)
) : Bitmap(width, height, bpp) {
	protected val temp = ByteArray(Math.max(width, height))

	val datau = UByteArray(data)
	val n8_dbpp: Int = 8 / bpp
	val mask = ((1 shl bpp) - 1)
	operator open fun get(x: Int, y: Int): Int = (datau[index_d(x, y)] ushr (bpp * index_m(x, y))) and mask
	operator open fun set(x: Int, y: Int, value: Int): Unit {
		val i = index_d(x, y)
		datau[i] = datau[i].insert(value, bpp * index_m(x, y), bpp)
	}
	override fun get32(x: Int, y: Int): Int = palette[this[x, y]]
	fun index_d(x: Int, y: Int) = index(x, y) / n8_dbpp
	fun index_m(x: Int, y: Int) = index(x, y) % n8_dbpp

	fun setRow(y: Int, row: UByteArray) {
		System.arraycopy(row.data, 0, data, index(0, y), stride)
	}

	fun setRow(y: Int, row: ByteArray) {
		System.arraycopy(row, 0, data, index(0, y), stride)
	}

	fun setWhitescalePalette() = this.apply {
		for (n in 0 until palette.size) {
			val col = ((n.toFloat() / palette.size.toFloat()) * 255).toInt()
			palette[n] = RGBA(col, col, col, 0xFF)
		}
		return this
	}

	override fun swapRows(y0: Int, y1: Int) {
		val s0 = index(0, y0)
		val s1 = index(0, y1)
		System.arraycopy(data, s0, temp, 0, stride)
		System.arraycopy(data, s1, data, s0, stride)
		System.arraycopy(temp, 0, data, s1, stride)
	}
}