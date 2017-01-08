package com.soywiz.korim.bitmap

import com.soywiz.korim.color.ColorFormat
import com.soywiz.korim.color.RGBA
import java.util.*

class Bitmap32(
	width: Int,
	height: Int,
	val data: IntArray = IntArray(width * height)
) : Bitmap(width, height, 32), Iterable<Int> {
	private val temp = IntArray(Math.max(width, height))

	constructor(width: Int, height: Int, generator: (x: Int, y: Int) -> Int) : this(width, height) {
		setEach(generator)
	}

	operator fun set(x: Int, y: Int, color: Int) = apply { data[index(x, y)] = color }
	operator fun get(x: Int, y: Int): Int = data[index(x, y)]
	override fun get32(x: Int, y: Int): Int = get(x, y)

	fun setRow(y: Int, row: IntArray) {
		System.arraycopy(row, 0, data, index(0, y), width)
	}

	fun _draw(src: Bitmap32, dx: Int, dy: Int, sleft: Int, stop: Int, sright: Int, sbottom: Int, mix: Boolean) {
		val dst = this
		val width = sright - sleft
		val height = sbottom - stop
		val dstData = dst.data
		val srcData = src.data
		for (y in 0 until height) {
			val dstOffset = dst.index(dx, dy + y)
			val srcOffset = src.index(sleft, stop + y)
			if (mix) {
				for (x in 0 until width) dstData[dstOffset + x] = RGBA.mix(dstData[dstOffset + x], srcData[srcOffset + x])
			} else {
				for (x in 0 until width) dstData[dstOffset + x] = srcData[srcOffset + x]
			}
		}
	}

	fun _drawPut(mix: Boolean, other: Bitmap32, _dx: Int = 0, _dy: Int = 0) {
		var dx = _dx
		var dy = _dy
		var sleft = 0
		var stop = 0
		val sright = other.width
		val sbottom = other.height
		if (dx < 0) {
			sleft = -dx
			//sright += dx
			dx = 0
		}
		if (dy < 0) {
			stop = -dy
			//sbottom += dy
			dy = 0
		}

		_draw(other, dx, dy, sleft, stop, sright, sbottom, mix)
	}

	fun fill(color: Int, x: Int = 0, y: Int = 0, width: Int = this.width, height: Int = this.height) {
		val x1 = clampX(x)
		val x2 = clampX(x + width - 1)
		val y1 = clampY(y)
		val y2 = clampY(y + height - 1)
		for (cy in y1..y2) Arrays.fill(this.data, index(x1, cy), index(x2, cy) + 1, color)
	}

	fun _draw(src: BitmapSlice<Bitmap32>, dx: Int = 0, dy: Int = 0, mix: Boolean) {
		val b = src.bounds

		val availableWidth = width - dx
		val availableHeight = height - dy

		val awidth = Math.min(availableWidth, b.width)
		val aheight = Math.min(availableHeight, b.height)

		_draw(src.bmp, dx, dy, b.x, b.y, b.x + awidth, b.y + aheight, mix = mix)
	}

	fun put(src: Bitmap32, dx: Int = 0, dy: Int = 0) = _drawPut(false, src, dx, dy)
	fun draw(src: Bitmap32, dx: Int = 0, dy: Int = 0) = _drawPut(true, src, dx, dy)

	fun put(src: BitmapSlice<Bitmap32>, dx: Int = 0, dy: Int = 0) = _draw(src, dx, dy, mix = false)
	fun draw(src: BitmapSlice<Bitmap32>, dx: Int = 0, dy: Int = 0) = _draw(src, dx, dy, mix = true)

	fun copySliceWithBounds(left: Int, top: Int, right: Int, bottom: Int): Bitmap32 = copySliceWithSize(left, top, right - left, bottom - top)

	fun copySliceWithSize(x: Int, y: Int, width: Int, height: Int): Bitmap32 {
		val out = Bitmap32(width, height)
		for (yy in 0 until height) for (xx in 0 until width) {
			out[xx, y] = this[x + xx, y + yy]
		}
		return out
	}

	inline fun forEach(callback: (n: Int, x: Int, y: Int) -> Unit) {
		var n = 0
		for (y in 0 until height) for (x in 0 until width) callback(n++, x, y)
	}

	inline fun setEach(callback: (x: Int, y: Int) -> Int) {
		forEach { n, x, y -> this.data[n] = callback(x, y) }
	}

	inline fun transformColor(callback: (rgba: Int) -> Int) {
		forEach { n, x, y -> this.data[n] = callback(this.data[n]) }
	}

	fun writeChannel(destination: BitmapChannel, input: Bitmap32, source: BitmapChannel) {
		val sourceShift = source.shift
		val destShift = destination.shift
		val destClear = destination.clearMask
		val thisData = this.data
		val inputData = input.data
		for (n in 0 until area) {
			val c = (inputData[n] ushr sourceShift) and 0xFF
			thisData[n] = (thisData[n] and destClear) or (c shl destShift)
		}
	}

	fun writeChannel(destination: BitmapChannel, input: Bitmap8) {
		val destShift = destination.index * 8
		val destClear = (0xFF shl destShift).inv()
		for (n in 0 until area) {
			val c = input.data[n].toInt() and 0xFF
			this.data[n] = (this.data[n] and destClear) or (c shl destShift)
		}
	}

	inline fun writeChannel(destination: BitmapChannel, gen: (x: Int, y: Int) -> Int) {
		val destShift = destination.index * 8
		val destClear = (0xFF shl destShift).inv()
		var n = 0
		for (y in 0 until height) {
			for (x in 0 until width) {
				val c = gen(x, y) and 0xFF
				this.data[n] = (this.data[n] and destClear) or (c shl destShift)
				n++
			}
		}
	}

	inline fun writeChannelN(destination: BitmapChannel, gen: (n: Int) -> Int) {
		val destShift = destination.index * 8
		val destClear = (0xFF shl destShift).inv()
		for (n in 0 until area) {
			val c = gen(n) and 0xFF
			this.data[n] = (this.data[n] and destClear) or (c shl destShift)
		}
	}

	fun extractChannel(channel: BitmapChannel): Bitmap8 {
		val out = Bitmap8(width, height)
		val shift = channel.shift
		for (n in 0 until area) {
			out.data[n] = ((data[n] ushr shift) and 0xFF).toByte()
		}
		return out
	}

	companion object {
		fun createWithAlpha(color: Bitmap32, alpha: Bitmap32, alphaChannel: BitmapChannel = BitmapChannel.RED): Bitmap32 {
			val out = Bitmap32(color.width, color.height)
			out.put(color)
			out.writeChannel(BitmapChannel.ALPHA, alpha, BitmapChannel.RED)
			return out
		}

		// https://en.wikipedia.org/wiki/Structural_similarity
		fun matchesSSMI(a: Bitmap, b: Bitmap): Boolean = TODO()

		fun matches(a: Bitmap, b: Bitmap, threshold: Int = 32): Boolean {
			val diff = diff(a, b)
			return diff.data.all {
				(RGBA.getR(it) < threshold) && (RGBA.getG(it) < threshold) &&
					(RGBA.getB(it) < threshold) && (RGBA.getA(it) < threshold)
			}
		}

		fun diff(a: Bitmap, b: Bitmap): Bitmap32 {
			if (a.width != b.width || a.height != b.height) throw IllegalArgumentException("$a not matches $b size")
			val a32 = a.toBMP32()
			val b32 = b.toBMP32()
			val out = Bitmap32(a.width, a.height)
			for (n in 0 until out.area) {
				val c1 = a32.data[n]
				val c2 = b32.data[n]

				val dr = Math.abs(RGBA.getR(c1) - RGBA.getR(c2))
				val dg = Math.abs(RGBA.getG(c1) - RGBA.getG(c2))
				val db = Math.abs(RGBA.getB(c1) - RGBA.getB(c2))
				val da = Math.abs(RGBA.getA(c1) - RGBA.getA(c2))

				out.data[n] = RGBA.pack(dr, dg, db, da)
			}
			return out
		}
	}

	fun invert() = xor(0x00FFFFFF)

	fun xor(value: Int) {
		for (n in 0 until area) data[n] = data[n] xor value
	}

	override fun toString(): String = "Bitmap32($width, $height)"

	override fun swapRows(y0: Int, y1: Int) {
		val s0 = index(0, y0)
		val s1 = index(0, y1)
		System.arraycopy(data, s0, temp, 0, width)
		System.arraycopy(data, s1, data, s0, width)
		System.arraycopy(temp, 0, data, s1, width)
	}

	fun writeDecoded(color: ColorFormat, data: ByteArray, offset: Int = 0, littleEndian: Boolean = true): Bitmap32 = this.apply {
		color.decode(data, offset, this.data, 0, this.area, littleEndian = littleEndian)
	}

	override fun iterator(): Iterator<Int> = data.iterator()
}