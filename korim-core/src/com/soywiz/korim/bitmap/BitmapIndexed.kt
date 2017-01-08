package com.soywiz.korim.bitmap

import com.soywiz.korio.util.UByteArray
import com.soywiz.korio.util.insert

abstract class BitmapIndexed(
	override val bpp: Int,
	width: Int, height: Int,
	var data: ByteArray = ByteArray(width * height / (8 / bpp)),
	var palette: IntArray = IntArray(1 shl bpp)
) : Bitmap(width, height) {
	val datau = UByteArray(data)
	val n8_dbpp: Int = 8 / bpp
	operator fun get(x: Int, y: Int): Int = (datau[index_d(x, y)] ushr (bpp * index_m(x, y))) and mask
	operator fun set(x: Int, y: Int, value: Int): Unit {
		val i = index_d(x, y)
		datau[i] = datau[i].insert(value, bpp * index_m(x, y), bpp)
	}
	override fun get32(x: Int, y: Int): Int = palette[this[x, y]]
	val mask = ((1 shl bpp) - 1)
	fun index_d(x: Int, y: Int) = index(x, y) / n8_dbpp
	fun index_m(x: Int, y: Int) = index(x, y) % n8_dbpp
}