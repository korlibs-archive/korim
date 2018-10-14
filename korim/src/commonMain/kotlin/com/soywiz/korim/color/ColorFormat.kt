package com.soywiz.korim.color

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.*
import kotlin.math.*

interface ColorFormatBase {
	fun getR(v: Int): Int
	fun getG(v: Int): Int
	fun getB(v: Int): Int
	fun getA(v: Int): Int
	fun pack(r: Int, g: Int, b: Int, a: Int): Int

	class Mixin(
		val rOffset: Int, val rSize: Int,
		val gOffset: Int, val gSize: Int,
		val bOffset: Int, val bSize: Int,
		val aOffset: Int, val aSize: Int
	) : ColorFormatBase {
		override fun getR(v: Int): Int = v.extractScaledFF(rOffset, rSize)
		override fun getG(v: Int): Int = v.extractScaledFF(gOffset, gSize)
		override fun getB(v: Int): Int = v.extractScaledFF(bOffset, bSize)
		override fun getA(v: Int): Int = v.extractScaledFFDefault(aOffset, aSize, default = 0xFF)
		override fun pack(r: Int, g: Int, b: Int, a: Int): Int {
			return 0
				.insertScaledFF(r, rOffset, rSize)
				.insertScaledFF(g, gOffset, gSize)
				.insertScaledFF(b, bOffset, bSize)
				.insertScaledFF(a, aOffset, aSize)
		}
	}
}

abstract class ColorFormat(val bpp: Int) : ColorFormatBase {
	val bytesPerPixel = bpp / 8

	fun getRf(v: Int): Float = getR(v).toFloat() / 255f
	fun getGf(v: Int): Float = getG(v).toFloat() / 255f
	fun getBf(v: Int): Float = getB(v).toFloat() / 255f
	fun getAf(v: Int): Float = getA(v).toFloat() / 255f

	fun getRd(v: Int): Double = getR(v).toDouble() / 255.0
	fun getGd(v: Int): Double = getG(v).toDouble() / 255.0
	fun getBd(v: Int): Double = getB(v).toDouble() / 255.0
	fun getAd(v: Int): Double = getA(v).toDouble() / 255.0

	//fun clamp0_FF(a: Int): Int = Math.min(Math.max(a, 0), 255)
	//fun clampFF(a: Int): Int = Math.min(a, 255)

	fun toRGBA(v: Int): RGBA = RGBA(getR(v), getG(v), getB(v), getA(v))
	fun toRGBAInt(v: Int): Int = RGBA.packFast(getR(v), getG(v), getB(v), getA(v))

	fun packRGBA(c: RGBA): Int = pack(c.r, c.g, c.b, c.a)
	fun packRGBAInt(c: Int): Int = pack(getR(c), getG(c), getB(c), getA(c))

	fun unpackToRGBA(packed: Int): RGBA = RGBA(getR(packed), getG(packed), getB(packed), getA(packed))
	fun unpackToRGBAInt(packed: Int): Int = RGBAInt(getR(packed), getG(packed), getB(packed), getA(packed))

	fun convertTo(color: Int, target: ColorFormat): Int = target.pack(
		this.getR(color), this.getG(color), this.getB(color), this.getA(color)
	)

	companion object {
		@JvmStatic
		fun clamp0_FF(v: Int) = if (v < 0x00) 0x00 else if (v > 0xFF) 0xFF else v

		@JvmStatic
		fun clampf01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v

		@JvmStatic
		fun clampFF(a: Int): Int = min(a, 255)
	}

	inline fun decodeInternal(
		data: ByteArray,
		dataOffset: Int,
		out: RgbaArray,
		outOffset: Int,
		size: Int,
		read: (data: ByteArray, io: Int) -> Int
	) {
		var io = dataOffset
		var oo = outOffset
		val bytesPerPixel = this.bytesPerPixel
		val outdata = out.array

		for (n in 0 until size) {
			val c = read(data, io)
			io += bytesPerPixel
			outdata[oo++] = RGBA.packFast(getR(c), getG(c), getB(c), getA(c))
		}
	}

	open fun decode(
		data: ByteArray,
		dataOffset: Int,
		out: RgbaArray,
		outOffset: Int,
		size: Int,
		littleEndian: Boolean = true
	) {
		when (bpp) {
			16 -> if (littleEndian) {
				decodeInternal(data, dataOffset, out, outOffset, size, ByteArray::readU16_le)
			} else {
				decodeInternal(data, dataOffset, out, outOffset, size, ByteArray::readU16_be)
			}
			24 -> if (littleEndian) {
				decodeInternal(data, dataOffset, out, outOffset, size, ByteArray::readU24_le)
			} else {
				decodeInternal(data, dataOffset, out, outOffset, size, ByteArray::readU24_be)
			}
			32 -> if (littleEndian) {
				decodeInternal(data, dataOffset, out, outOffset, size, ByteArray::readS32_le)
			} else {
				decodeInternal(data, dataOffset, out, outOffset, size, ByteArray::readS32_be)
			}
			else -> throw IllegalArgumentException("Unsupported bpp $bpp")
		}
	}

	open fun decode(
		data: ByteArray,
		dataOffset: Int = 0,
		size: Int = data.size / bytesPerPixel,
		littleEndian: Boolean = true
	): RgbaArray {
		val out = RgbaArray(size)
		decode(data, dataOffset, out, 0, size, littleEndian)
		return out
	}

	open fun decodeToBitmap32(
		width: Int,
		height: Int,
		data: ByteArray,
		dataOffset: Int = 0,
		littleEndian: Boolean = true
	): Bitmap32 {
		return Bitmap32(width, height, decode(data, dataOffset, width * height, littleEndian))
	}

	open fun decodeToBitmap32(
		bmp: Bitmap32,
		data: ByteArray,
		dataOffset: Int = 0,
		littleEndian: Boolean = true
	): Bitmap32 {
		return bmp.apply { decode(data, dataOffset, this.data, 0, bmp.area) }
	}

	open fun encode(
		colors: RgbaArray,
		colorsOffset: Int,
		out: ByteArray,
		outOffset: Int,
		size: Int,
		littleEndian: Boolean = true
	) {
		var io = colorsOffset
		var oo = outOffset
		for (n in 0 until size) {
			val c = colors.array[io++]
			val ec = pack(RGBA.getR(c), RGBA.getG(c), RGBA.getB(c), RGBA.getA(c))
			when (bpp) {
				16 -> if (littleEndian) out.write16_le(oo, ec) else out.write16_be(oo, ec)
				24 -> if (littleEndian) out.write24_le(oo, ec) else out.write24_be(oo, ec)
				32 -> if (littleEndian) out.write32_le(oo, ec) else out.write32_be(oo, ec)
				else -> throw IllegalArgumentException("Unsupported bpp $bpp")
			}
			oo += bytesPerPixel
		}
	}

	open fun encode(
		colors: RgbaArray,
		colorsOffset: Int = 0,
		size: Int = colors.size,
		littleEndian: Boolean = true
	): ByteArray {
		val out = ByteArray(size * bytesPerPixel)
		encode(colors, colorsOffset, out, 0, size, littleEndian)
		return out
	}
}