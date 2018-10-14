package com.soywiz.korim.bitmap

import com.soywiz.kmem.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.util.*
import kotlin.math.*

class Bitmap32(
	width: Int,
	height: Int,
	val data: RgbaArray = RgbaArray(width * height),
	premult: Boolean = false
) : Bitmap(width, height, 32, premult, data), Iterable<RGBA> {
	init {
		if (data.size < width * height) throw RuntimeException("Bitmap data is too short: width=$width, height=$height, data=ByteArray(${data.size}), area=${width * height}")
	}

	private val temp = RgbaArray(max(width, height))

	//constructor(width: Int, height: Int, value: Int, premultiplied: Boolean = false) : this(width, height, IntArray(width * height) { value }, premultiplied)
	constructor(width: Int, height: Int, value: RGBA, premult: Boolean) : this(width, height, premult = premult) {
		data.fill(value)
	}

	@Deprecated("Use premultiplied constructor instead")
	constructor(width: Int, height: Int, value: RGBA) : this(width, height, premult = false) {
		data.fill(value)
	}

	constructor(width: Int, height: Int, premult: Boolean = false, generator: (x: Int, y: Int) -> RGBA)
			: this(width, height, premult = premult)
	{
		setEach(generator)
	}

	override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap32(width, height, premult = premult)

	override fun copy(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) {
		val src = this

		val srcArray = src.data
		var srcIndex = src.index(srcX, srcY)
		val srcAdd = src.width

		val dstArray = (dst as Bitmap32).data
		var dstIndex = dst.index(dstX, dstY)
		val dstAdd = dst.width

		for (y in 0 until height) {
			arraycopy(srcArray.array, srcIndex, dstArray.array, dstIndex, width)
			srcIndex += srcAdd
			dstIndex += dstAdd
		}
	}
	operator fun set(x: Int, y: Int, color: RGBA) = run { data[index(x, y)] = color }
	operator fun get(x: Int, y: Int): RGBA = data[index(x, y)]
	override fun setInt(x: Int, y: Int, color: Int) = run { data.array[index(x, y)] = RGBAInt(color) }
	override fun getInt(x: Int, y: Int): Int = data.array[index(x, y)]
	override fun get32Int(x: Int, y: Int): Int = data.array[index(x, y)]
	override fun set32Int(x: Int, y: Int, v: Int): Unit = run { data.array[index(x, y)] = v }

	fun setRow(y: Int, row: IntArray) {
		arraycopy(row, 0, data.array, index(0, y), width)
	}

	fun _draw(src: Bitmap32, dx: Int, dy: Int, sleft: Int, stop: Int, sright: Int, sbottom: Int, mix: Boolean) {
		val dst = this
		val width = sright - sleft
		val height = sbottom - stop
		val dstData = dst.data.array
		val srcData = src.data.array
		for (y in 0 until height) {
			val dstOffset = dst.index(dx, dy + y)
			val srcOffset = src.index(sleft, stop + y)
			if (mix) {
				for (x in 0 until width) dstData[dstOffset + x] =
						RGBA.mixInt(dstData[dstOffset + x], srcData[srcOffset + x])
			} else {
				// System.arraycopy
				arraycopy(srcData, srcOffset, dstData, dstOffset, width)
				//for (x in 0 until width) dstData[dstOffset + x] = srcData[srcOffset + x]
			}
		}
	}

	fun drawPixelMixed(x: Int, y: Int, c: RGBA) {
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

	fun fill(color: RGBA, x: Int = 0, y: Int = 0, width: Int = this.width, height: Int = this.height) {
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

	fun drawUnoptimized(src: BitmapSlice<Bitmap>, dx: Int = 0, dy: Int = 0, mix: Boolean = true) {
		if (src.bmp is Bitmap32) {
			_draw(src as BitmapSlice<Bitmap32>, dx, dy, mix = mix)
		} else {
			drawUnoptimized(src.bmp, dx, dy, src.left, src.top, src.right, src.bottom, mix = mix)
		}
	}

	fun drawUnoptimized(src: Bitmap, dx: Int, dy: Int, sleft: Int, stop: Int, sright: Int, sbottom: Int, mix: Boolean) {
		val dst = this
		val width = sright - sleft
		val height = sbottom - stop
		val dstData = dst.data.array
		for (y in 0 until height) {
			val dstOffset = dst.index(dx, dy + y)
			if (mix) {
				for (x in 0 until width) dstData[dstOffset + x] = RGBA.mixInt(dstData[dstOffset + x], src.get32Int(sleft + x, stop + y))
			} else {
				for (x in 0 until width) dstData[dstOffset + x] = src.get32Int(sleft + x, stop + y)
			}
		}
	}

	fun copySliceWithBounds(left: Int, top: Int, right: Int, bottom: Int): Bitmap32 =
		copySliceWithSize(left, top, right - left, bottom - top)

	fun copySliceWithSize(x: Int, y: Int, width: Int, height: Int): Bitmap32 {
		val out = Bitmap32(width, height)
		for (yy in 0 until height) {
			arraycopy(this.data, this.index(x, y + yy), out.data, out.index(0, yy), width)
		}
		return out
	}

	inline fun all(callback: (RGBA) -> Boolean): Boolean {
		return (0 until area).any { callback(data[it]) }
	}

	inline fun forEach(callback: (n: Int, x: Int, y: Int) -> Unit) {
		var n = 0
		for (y in 0 until height) for (x in 0 until width) callback(n++, x, y)
	}

	inline fun setEach(callback: (x: Int, y: Int) -> RGBA) {
		forEach { n, x, y -> this.data[n] = callback(x, y) }
	}

	inline fun transformColor(callback: (rgba: RGBA) -> RGBA) {
		forEach { n, x, y -> this.data[n] = callback(this.data[n]) }
	}

	fun writeChannel(destination: BitmapChannel, input: Bitmap32, source: BitmapChannel) {
		val sourceShift = source.shift
		val destShift = destination.shift
		val destClear = destination.clearMask
		val thisData = this.data
		val inputData = input.data
		for (n in 0 until area) {
			val c = (inputData.array[n] ushr sourceShift) and 0xFF
			thisData.array[n] = RGBAInt((thisData.array[n] and destClear) or (c shl destShift))
		}
	}

	fun writeChannel(destination: BitmapChannel, input: Bitmap8) {
		val destShift = destination.index * 8
		val destClear = (0xFF shl destShift).inv()
		for (n in 0 until area) {
			val c = input.data[n].toInt() and 0xFF
			this.data.array[n] = RGBAInt((this.data.array[n] and destClear) or (c shl destShift))
		}
	}

	inline fun writeChannel(destination: BitmapChannel, gen: (x: Int, y: Int) -> Int) {
		val destShift = destination.index * 8
		val destClear = (0xFF shl destShift).inv()
		var n = 0
		for (y in 0 until height) {
			for (x in 0 until width) {
				val c = gen(x, y) and 0xFF
				this.data.array[n] = RGBAInt((this.data.array[n] and destClear) or (c shl destShift))
				n++
			}
		}
	}

	inline fun writeChannelN(destination: BitmapChannel, gen: (n: Int) -> Int) {
		val destShift = destination.index * 8
		val destClear = (0xFF shl destShift).inv()
		for (n in 0 until area) {
			val c = gen(n) and 0xFF
			this.data.array[n] = RGBAInt((this.data.array[n] and destClear) or (c shl destShift))
		}
	}

	fun extractChannel(channel: BitmapChannel): Bitmap8 {
		val out = Bitmap8(width, height)
		val shift = channel.shift
		for (n in 0 until area) {
			out.data[n] = ((data.array[n] ushr shift) and 0xFF).toByte()
		}
		return out
	}

	companion object {
		operator fun invoke(width: Int, height: Int, premult: Boolean = false, generator: (x: Int, y: Int) -> Int): Bitmap32 {
			val data = IntArray(width * height)
			var n = 0
			for (y in 0 until height) {
				for (x in 0 until width) {
					data[n++] = generator(x, y)
				}
			}
			return Bitmap32(width, height, RgbaArray(data), premult)
		}

		fun copyRect(
			src: Bitmap32,
			srcX: Int,
			srcY: Int,
			dst: Bitmap32,
			dstX: Int,
			dstY: Int,
			width: Int,
			height: Int
		) {
			for (y in 0 until height) {
				val srcIndex = src.index(srcX, srcY + y)
				val dstIndex = dst.index(dstX, dstY + y)
				arraycopy(src.data, srcIndex, dst.data, dstIndex, width)
			}
		}

		fun createWithAlpha(
			color: Bitmap32,
			alpha: Bitmap32,
			alphaChannel: BitmapChannel = BitmapChannel.RED
		): Bitmap32 {
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
				(it.r < threshold) && (it.g < threshold) && (it.b < threshold) && (it.a < threshold)
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
				val c1 = RGBA.premultiplyFastInt(a32.data.array[n])
				val c2 = RGBA.premultiplyFastInt(b32.data.array[n])

				val dr = abs(RGBA.getR(c1) - RGBA.getR(c2))
				val dg = abs(RGBA.getG(c1) - RGBA.getG(c2))
				val db = abs(RGBA.getB(c1) - RGBA.getB(c2))
				val da = abs(RGBA.getA(c1) - RGBA.getA(c2))
				//val da = 0

				//println("%02X, %02X, %02X".format(RGBA.getR(c1), RGBA.getR(c2), dr))
				out.data.array[n] = RGBAInt(dr, dg, db, da)

				//println("$dr, $dg, $db, $da : ${out.data[n]}")
			}
			//showImageAndWait(out)
			return out
		}
	}

	fun invert() = xor(RGBA(255, 255, 255, 0))

	fun xor(value: RGBA) {
		for (n in 0 until area) data.array[n] = RGBAInt(data.array[n] xor value.rgba)
	}

	override fun toString(): String = "Bitmap32($width, $height)"

	override fun swapRows(y0: Int, y1: Int) {
		val s0 = index(0, y0)
		val s1 = index(0, y1)
		arraycopy(data, s0, temp, 0, width)
		arraycopy(data, s1, data, s0, width)
		arraycopy(temp, 0, data, s1, width)
	}

	fun writeDecoded(color: ColorFormat, data: ByteArray, offset: Int = 0, littleEndian: Boolean = true): Bitmap32 =
		this.apply {
			color.decode(data, offset, this.data, 0, this.area, littleEndian = littleEndian)
		}

	override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(Bitmap32Context2d(this, antialiasing))

	fun clone() = Bitmap32(width, height, RgbaArray(this.data.array.copyOf()), premult)

	fun premultipliedIfRequired(): Bitmap32 = if (this.premult) this else premultiplied()
	fun depremultipliedIfRequired(): Bitmap32 = if (!this.premult) this else depremultiplied()
	fun premultiplied(): Bitmap32 = this.clone().apply { premultiplyInplace() }
	fun depremultiplied(): Bitmap32 = this.clone().apply { depremultiplyInplace() }

	fun premultiplyInplace(): Bitmap32 {
		if (premult) return this
		premult = true
		val array = data.array
		//for (n in 0 until array.size) array[n] = RGBA.premultiplyFastInt(array[n])
		for (n in 0 until array.size) array[n] = RGBA.premultiplyAccurate(array[n])
		return this
	}

	fun depremultiplyInplace(): Bitmap32 {
		if (!premult) return this
		premult = false
		val array = data.array
		for (n in 0 until array.size) array[n] = RGBA.depremultiplyFastInt(array[n])
		//for (n in 0 until data.size) data[n] = RGBA.depremultiplyAccurate(data[n])
		return this
	}

	fun applyTransform(ct: ColorTransform): Bitmap32 = clone().apply { applyTransformInline(ct) }

	fun applyTransformInline(ct: ColorTransform) {
		val R = IntArray(256) { ((it * ct.mR) + ct.aR).toInt().clamp(0x00, 0xFF) }
		val G = IntArray(256) { ((it * ct.mG) + ct.aG).toInt().clamp(0x00, 0xFF) }
		val B = IntArray(256) { ((it * ct.mB) + ct.aB).toInt().clamp(0x00, 0xFF) }
		val A = IntArray(256) { ((it * ct.mA) + ct.aA).toInt().clamp(0x00, 0xFF) }
		for (n in 0 until data.size) {
			val c = data.array[n]
			data.array[n] = RGBAInt(R[RGBA.getR(c)], G[RGBA.getG(c)], B[RGBA.getB(c)], A[RGBA.getA(c)])
		}
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
					val c1 = dst.array[m]
					val c2 = dst.array[m + 1]
					val c3 = dst.array[m + width]
					val c4 = dst.array[m + width + 1]
					dst.array[n] = RGBAInt(RGBA.blendRGBAFastAlreadyPremultiplied_05(c1, c2, c3, c4))
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

	override fun iterator(): Iterator<RGBA> = data.iterator()

	fun setRowChunk(x: Int, y: Int, data: RgbaArray, width: Int, increment: Int) {
		if (increment == 1) {
			arraycopy(data, 0, this.data, index(x, y), width)
		} else {
			var m = index(x, y)
			for (n in 0 until width) {
				this.data.array[m] = data.array[n]
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
			data.array[n] = dstCmp.insertInt(data.array[n], srcCmp.extractInt(fdata.array[n]))
		}
	}

	fun rgbaToYCbCr(): Bitmap32 = Bitmap32(width, height).apply {
		for (n in 0 until area) this.data.array[n] = RGBAInt(YCbCr.rgbaToYCbCrInt(this@Bitmap32.data.array[n]))
	}

	fun yCbCrToRgba(): Bitmap32 = Bitmap32(width, height).apply {
		for (n in 0 until area) this.data.array[n] = YCbCr.yCbCrToRgbaInt(this@Bitmap32.data.array[n])
	}

	fun computeHash(): Int {
		var hash = 0
		for (n in 0 until data.size) hash += data.array[n]
		return hash
	}
}

fun Bitmap32Int(width: Int, height: Int, premult: Boolean = false, generator: (x: Int, y: Int) -> Int): Bitmap32 {
	val out = Bitmap32(width, height, Colors.TRANSPARENT_BLACK, premult)
	val aout = out.data.array
	var n = 0
	for (y in 0 until height) {
		for (x in 0 until width) {
			aout[n++] = generator(x, y)
		}
	}
	return out
}
