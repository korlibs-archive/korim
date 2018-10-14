package com.soywiz.korim.bitmap

import com.soywiz.kmem.*
import com.soywiz.korim.color.*
import kotlin.math.*

abstract class BitmapIndexed(
	bpp: Int,
	width: Int, height: Int,
	var data: ByteArray = ByteArray(width * height / (8 / bpp)),
	var palette: RgbaArray = RgbaArray(1 shl bpp)
) : Bitmap(width, height, bpp, false, data) {
	init {
		if (data.size < width * height / (8 / bpp)) throw RuntimeException("Bitmap data is too short: width=$width, height=$height, data=ByteArray(${data.size}), area=${width * height}")
	}

	override fun toString() = "BitmapIndexed(bpp=$bpp, width=$width, height=$height, clut=${palette.size})"

	protected val temp = ByteArray(max(width, height))

	val datau = UByteArray(data)
	val n8_dbpp: Int = 8 / bpp
	val mask = ((1 shl bpp) - 1)

	inline operator fun get(x: Int, y: Int): Int = getInt(x, y)
	inline operator fun set(x: Int, y: Int, color: Int): Unit = setInt(x, y, color)

	override fun getInt(x: Int, y: Int): Int = (datau[index_d(x, y)] ushr (bpp * index_m(x, y))) and mask
	override fun setInt(x: Int, y: Int, color: Int): Unit {
		val i = index_d(x, y)
		datau[i] = datau[i].insert(color, bpp * index_m(x, y), bpp)
	}

	override fun get32Int(x: Int, y: Int): Int = palette.array[this[x, y]]
	fun index_d(x: Int, y: Int) = index(x, y) / n8_dbpp
	fun index_m(x: Int, y: Int) = index(x, y) % n8_dbpp

	fun setRow(y: Int, row: UByteArray) {
		arraycopy(row.data, 0, data, index(0, y), stride)
	}

	fun setRow(y: Int, row: ByteArray) {
		arraycopy(row, 0, data, index(0, y), stride)
	}

	fun setWhitescalePalette() = this.apply {
		for (n in 0 until palette.size) {
			val col = ((n.toFloat() / palette.size.toFloat()) * 255).toInt()
			palette.array[n] = RGBA.packFast(col, col, col, 0xFF)
		}
		return this
	}

	override fun swapRows(y0: Int, y1: Int) {
		val s0 = index_d(0, y0)
		val s1 = index_d(0, y1)
		arraycopy(data, s0, temp, 0, stride)
		arraycopy(data, s1, data, s0, stride)
		arraycopy(temp, 0, data, s1, stride)
	}

	fun toLines(palette: String): List<String> {
		return (0 until height).map { y -> (0 until height).map { x -> palette[this[x, y]] }.joinToString("") }
	}

	fun setRowChunk(x: Int, y: Int, data: ByteArray, width: Int, increment: Int) {
		if (increment == 1) {
			arraycopy(data, 0, this.data, index(x, y), width / n8_dbpp)
		} else {
			var m = index(x, y)
			for (n in 0 until width / n8_dbpp) {
				this.data[m] = data[n]
				m += increment
			}
		}
	}

	override fun toBMP32(): Bitmap32 = Bitmap32(width, height, premult = premult).also { outBmp ->
		val out = outBmp.data.array
		val inp = this@BitmapIndexed.data
		val pal = this@BitmapIndexed.palette.array
		for (n in 0 until min(out.size, inp.size)) out[n] = pal[inp[n].toUnsigned()]
	}
}