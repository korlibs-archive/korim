package com.soywiz.korim.bitmap

import com.soywiz.korim.color.RGBA
import com.soywiz.korim.geom.IRect
import java.util.*

class Bitmap32(
	width: Int,
	height: Int,
	val data: IntArray = IntArray(width * height)
) : Bitmap(width, height), Iterable<Int> {
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
		for (y in 0 until height) {
			val dstOffset = dst.index(dx, dy + y)
			val srcOffset = src.index(sleft, stop + y)
			if (mix) {
				for (x in 0 until width) dst.data[dstOffset + x] = RGBA.mix(dst.data[dstOffset + x], src.data[srcOffset + x])
			} else {
				for (x in 0 until width) dst.data[dstOffset + x] = src.data[srcOffset + x]
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

	fun _draw(src: Bitmap32Slice, dx: Int = 0, dy: Int = 0, mix: Boolean) {
		val b = src.bounds

		val availableWidth = width - dx
		val availableHeight = height - dy

		val awidth = Math.min(availableWidth, b.width)
		val aheight = Math.min(availableHeight, b.height)

		_draw(src.bmp, dx, dy, b.x, b.y, b.x + awidth, b.y + aheight, mix = mix)
	}

	fun put(src: Bitmap32, dx: Int = 0, dy: Int = 0) = _drawPut(false, src, dx, dy)
	fun draw(src: Bitmap32, dx: Int = 0, dy: Int = 0) = _drawPut(true, src, dx, dy)

	fun put(src: Bitmap32Slice, dx: Int = 0, dy: Int = 0) = _draw(src, dx, dy, mix = false)
	fun draw(src: Bitmap32Slice, dx: Int = 0, dy: Int = 0) = _draw(src, dx, dy, mix = true)

	fun copySliceWithBounds(left: Int, top: Int, right: Int, bottom: Int): Bitmap32 = copySliceWithSize(left, top, right - left, bottom - top)

	fun copySliceWithSize(x: Int, y: Int, width: Int, height: Int): Bitmap32 {
		val out = Bitmap32(width, height)
		for (yy in 0 until height) for (xx in 0 until width) {
			out[xx, y] = this[x + xx, y + yy]
		}
		return out
	}

	fun sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int): Bitmap32Slice = Bitmap32Slice(this, IRect(left, top, right - left, bottom - top))
	fun sliceWithSize(x: Int, y: Int, width: Int, height: Int): Bitmap32Slice = Bitmap32Slice(this, IRect(x, y, width, height))

	fun slice(bounds: IRect): Bitmap32Slice = Bitmap32Slice(this, bounds)

	inline fun forEach(callback: (n: Int, x: Int, y: Int) -> Unit) {
		var n = 0
		for (y in 0 until height) for (x in 0 until width) callback(n++, x, y)
	}

	inline fun setEach(callback: (x: Int, y: Int) -> Int) {
		forEach { n, x, y -> this.data[n] = callback(x, y) }
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
	}

	fun invert() = xor(0x00FFFFFF)

	fun xor(value: Int) {
		for (n in 0 until area) data[n] = data[n] xor value
	}

	override fun toString(): String = "Bitmap32($width, $height)"

	fun flipY() {
		for (y in 0 until height / 2) swapRows(y, height - y - 1)
	}

	fun swapRows(y0: Int, y1: Int) {
		val s0 = index(0, y0)
		val s1 = index(0, y1)
		System.arraycopy(data, s0, temp, 0, width)
		System.arraycopy(data, s1, data, s0, width)
		System.arraycopy(temp, 0, data, s1, width)
	}

	override fun iterator(): Iterator<Int> = data.iterator()
}