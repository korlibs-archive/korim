package com.soywiz.korim.bitmap

import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.fill
import com.soywiz.korim.color.ColorFormat
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.YCbCr
import com.soywiz.korim.vector.Bitmap32Context2d
import com.soywiz.korim.vector.Context2d
import kotlin.math.abs
import kotlin.math.max

class Bitmap32(
	width: Int,
	height: Int,
	val data: IntArray = IntArray(width * height),
	premult: Boolean = false
) : Bitmap(width, height, 32, premult), Iterable<Int> {
	init {
		if (data.size < width * height) throw RuntimeException("Bitmap data is too short: width=$width, height=$height, data=ByteArray(${data.size}), area=${width * height}")
	}

	private val temp = IntArray(max(width, height))

	//constructor(width: Int, height: Int, value: Int, premultiplied: Boolean = false) : this(width, height, IntArray(width * height) { value }, premultiplied)
	constructor(width: Int, height: Int, value: Int, premult: Boolean) : this(width, height, premult = premult) {
		data.fill(value)
	}

	@Deprecated("Use premultiplied constructor instead")
	constructor(width: Int, height: Int, value: Int) : this(width, height, premult = false) {
		data.fill(value)
	}

	constructor(width: Int, height: Int, premult: Boolean = false, generator: (x: Int, y: Int) -> Int) : this(width, height, premult = premult) {
		setEach(generator)
	}

	override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap32(width, height, premult = premult)

	override operator fun set(x: Int, y: Int, color: Int) = run { data[index(x, y)] = color }
	override operator fun get(x: Int, y: Int): Int = data[index(x, y)]
	override fun get32(x: Int, y: Int): Int = get(x, y)
	override fun set32(x: Int, y: Int, v: Int): Unit = set(x, y, v)

	fun setRow(y: Int, row: IntArray) {
		arraycopy(row, 0, data, index(0, y), width)
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
				// System.arraycopy
				arraycopy(srcData, srcOffset, dstData, dstOffset, width)
				//for (x in 0 until width) dstData[dstOffset + x] = srcData[srcOffset + x]
			}
		}
	}

	fun drawPixelMixed(x: Int, y: Int, c: Int) {
		this[x, y] = RGBA.mix(this[x, y], c)
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
		for (cy in y1..y2) this.data.fill(color, index(x1, cy), index(x2, cy) + 1)
	}

	fun _draw(src: BitmapSlice<Bitmap32>, dx: Int = 0, dy: Int = 0, mix: Boolean) {
		val b = src.bounds

		val availableWidth = width - dx
		val availableHeight = height - dy

		val awidth = kotlin.math.min(availableWidth, b.width)
		val aheight = kotlin.math.min(availableHeight, b.height)

		_draw(src.bmp, dx, dy, b.x, b.y, b.x + awidth, b.y + aheight, mix = mix)
	}

	fun put(src: Bitmap32, dx: Int = 0, dy: Int = 0) = _drawPut(false, src, dx, dy)
	fun draw(src: Bitmap32, dx: Int = 0, dy: Int = 0) = _drawPut(true, src, dx, dy)

	fun put(src: BitmapSlice<Bitmap32>, dx: Int = 0, dy: Int = 0) = _draw(src, dx, dy, mix = false)
	fun draw(src: BitmapSlice<Bitmap32>, dx: Int = 0, dy: Int = 0) = _draw(src, dx, dy, mix = true)

	fun copySliceWithBounds(left: Int, top: Int, right: Int, bottom: Int): Bitmap32 = copySliceWithSize(left, top, right - left, bottom - top)

	fun copySliceWithSize(x: Int, y: Int, width: Int, height: Int): Bitmap32 {
		val out = Bitmap32(width, height)
		for (yy in 0 until height) {
			arraycopy(this.data, this.index(x, y + yy), out.data, out.index(0, yy), width)
		}
		return out
	}

	inline fun all(callback: (Int) -> Boolean): Boolean {
		return (0 until area).any { callback(data[it]) }
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
		fun copyRect(src: Bitmap32, srcX: Int, srcY: Int, dst: Bitmap32, dstX: Int, dstY: Int, width: Int, height: Int) {
			for (y in 0 until height) {
				val srcIndex = src.index(srcX, srcY + y)
				val dstIndex = dst.index(dstX, dstY + y)
				arraycopy(src.data, srcIndex, dst.data, dstIndex, width)
			}
		}

		fun createWithAlpha(color: Bitmap32, alpha: Bitmap32, alphaChannel: BitmapChannel = BitmapChannel.RED): Bitmap32 {
			val out = Bitmap32(color.width, color.height)
			out.put(color)
			out.writeChannel(BitmapChannel.ALPHA, alpha, BitmapChannel.RED)
			return out
		}

		// https://en.wikipedia.org/wiki/Structural_similarity
		suspend fun matchesSSMI(a: Bitmap, b: Bitmap): Boolean = TODO()

		suspend fun matches(a: Bitmap, b: Bitmap, threshold: Int = 32): Boolean {
			val diff = diff(a, b)
			//for (c in diff.data) println("%02X, %02X, %02X".format(RGBA.getR(c), RGBA.getG(c), RGBA.getB(c)))
			return diff.data.all {
				(RGBA.getR(it) < threshold) && (RGBA.getG(it) < threshold) && (RGBA.getB(it) < threshold) && (RGBA.getA(it) < threshold)
			}
		}

		fun diff(a: Bitmap, b: Bitmap): Bitmap32 {
			if (a.width != b.width || a.height != b.height) throw IllegalArgumentException("$a not matches $b size")
			val a32 = a.toBMP32()
			val b32 = b.toBMP32()
			val out = Bitmap32(a.width, a.height, premult = true)
			//showImageAndWait(a32)
			//showImageAndWait(b32)
			for (n in 0 until out.area) {
				val c1 = RGBA.premultiplyFast(a32.data[n])
				val c2 = RGBA.premultiplyFast(b32.data[n])

				val dr = abs(RGBA.getR(c1) - RGBA.getR(c2))
				val dg = abs(RGBA.getG(c1) - RGBA.getG(c2))
				val db = abs(RGBA.getB(c1) - RGBA.getB(c2))
				val da = abs(RGBA.getA(c1) - RGBA.getA(c2))
				//val da = 0

				//println("%02X, %02X, %02X".format(RGBA.getR(c1), RGBA.getR(c2), dr))
				out.data[n] = RGBA.pack(dr, dg, db, da)

				//println("$dr, $dg, $db, $da : ${out.data[n]}")
			}
			//showImageAndWait(out)
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
		arraycopy(data, s0, temp, 0, width)
		arraycopy(data, s1, data, s0, width)
		arraycopy(temp, 0, data, s1, width)
	}

	fun writeDecoded(color: ColorFormat, data: ByteArray, offset: Int = 0, littleEndian: Boolean = true): Bitmap32 = this.apply {
		color.decode(data, offset, this.data, 0, this.area, littleEndian = littleEndian)
	}

	override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(Bitmap32Context2d(this))

	fun clone() = Bitmap32(width, height, this.data.copyOf(), premult)

	fun premultipliedIfRequired(): Bitmap32 = if (this.premult) this else premultiplied()
	fun depremultipliedIfRequired(): Bitmap32 = if (!this.premult) this else depremultiplied()
	fun premultiplied(): Bitmap32 = this.clone().apply { premultiplyInplace() }
	fun depremultiplied(): Bitmap32 = this.clone().apply { depremultiplyInplace() }

	fun premultiplyInplace() {
		if (premult) return
		premult = true
		for (n in 0 until data.size) data[n] = RGBA.premultiplyFast(data[n])
	}

	fun depremultiplyInplace() {
		if (!premult) return
		premult = false
		for (n in 0 until data.size) data[n] = RGBA.depremultiplyFast(data[n])
		//for (n in 0 until data.size) data[n] = RGBA.depremultiplyAccurate(data[n])
	}

	/*
	// @TODO: Optimize memory usage
	private fun mipmapInplace(levels: Int): Bitmap32 {
		var cwidth = width
		var cheight = height
		for (level in 0 until levels) {
			cwidth /= 2
			for (y in 0 until cheight) {
				RGBA.downScaleBy2AlreadyPremultiplied(
					data, index(0, y), 1,
					data, index(0, y), 1,
					cwidth
				)
			}
			cheight /= 2
			for (x in 0 until cwidth) {
				RGBA.downScaleBy2AlreadyPremultiplied(
					data, index(x, 0), width,
					data, index(x, 0), width,
					cheight
				)
			}
		}

		return this
	}

	fun mipmap(levels: Int): Bitmap32 {
		val divide = Math.pow(2.0, levels.toDouble()).toInt()
		//val owidth =
		val temp = Bitmap32(width, height, this.data.copyOf(), this.premultiplied)
		val out = Bitmap32(width / divide, height / divide, premultiplied = true)
		temp.premultiplyInplace()
		temp.mipmapInplace(levels)
		Bitmap32.copyRect(temp, 0, 0, out, 0, 0, out.width, out.height)
		out.depremultiplyInplace()
		//return temp
		return out
	}
	*/

	fun mipmap(levels: Int): Bitmap32 {
		val temp = this.clone()
		temp.premultiplyInplace()
		val dst = temp.data

		var twidth = width
		var theight = height

		for (level in 0 until levels) {
			twidth /= 2
			theight /= 2
			for (y in 0 until theight) {
				var n = temp.index(0, y)
				var m = temp.index(0, y * 2)

				for (x in 0 until twidth) {
					val c1 = dst[m]
					val c2 = dst[m + 1]
					val c3 = dst[m + width]
					val c4 = dst[m + width + 1]
					dst[n] = RGBA.blendRGBAFastAlreadyPremultiplied_05(c1, c2, c3, c4)
					m += 2
					n++
				}
			}
		}
		val out = Bitmap32(twidth, theight, premult = true)
		Bitmap32.copyRect(temp, 0, 0, out, 0, 0, twidth, theight)
		//out.depremultiplyInplace()
		return out
	}

	override fun iterator(): Iterator<Int> = data.iterator()

	fun setRowChunk(x: Int, y: Int, data: IntArray, width: Int, increment: Int) {
		if (increment == 1) {
			arraycopy(data, 0, this.data, index(x, y), width)
		} else {
			var m = index(x, y)
			for (n in 0 until width) {
				this.data[m] = data[n]
				m += increment
			}
		}
	}

	fun extractBytes(): ByteArray = RGBA.encode(data)

	fun scaleNearest(sx: Int, sy: Int): Bitmap32 {
		val out = Bitmap32(width * sx, height * sy)
		for (y in 0 until out.height) {
			for (x in 0 until out.width) {
				out[x, y] = this[x / sx, y / sy]
			}
		}
		return out
	}

	fun writeComponent(dstCmp: BitmapChannel, from: Bitmap32, srcCmp: BitmapChannel) {
		val fdata = from.data
		for (n in 0 until area) {
			data[n] = dstCmp.insert(data[n], srcCmp.extract(fdata[n]))
		}
	}

	fun rgbaToYCbCr(): Bitmap32 = Bitmap32(width, height).apply { for (n in 0 until area) this.data[n] = YCbCr.rgbaToYCbCr(this@Bitmap32.data[n]) }
	fun yCbCrToRgba(): Bitmap32 = Bitmap32(width, height).apply { for (n in 0 until area) this.data[n] = YCbCr.yCbCrToRgba(this@Bitmap32.data[n]) }
}
