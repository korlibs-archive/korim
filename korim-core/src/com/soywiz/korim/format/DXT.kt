package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.BGR_565
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.util.readS32_le
import com.soywiz.korio.util.readU16_le
import com.soywiz.korio.util.readU32_le
import com.soywiz.korio.util.readU8
import com.soywiz.korio.vfs.PathInfo

// https://en.wikipedia.org/wiki/S3_Texture_Compression
open class DXT1 : DXT1Base("dxt1", premultiplied = true)
open class DXT2 : DXT2_3("dxt2", premultiplied = true)
open class DXT3 : DXT2_3("dxt3", premultiplied = false)
open class DXT4 : DXT4_5("dxt4", premultiplied = true)
open class DXT5 : DXT4_5("dxt5", premultiplied = false)

open class DXT1Base(format: String, premultiplied: Boolean) : DXT(format, premultiplied = true, blockSize = 8) {
	override fun decodeRow(data: ByteArray, dataOffset: Int, bmp: IntArray, bmpOffset: Int, bmpStride: Int) {
		decodeDxt1ColorCond(data, dataOffset + 0, cc)
		val cdata = data.readS32_le(dataOffset + 4)
		var pos = bmpOffset
		var n = 0
		for (y in 0 until 4) {
			for (x in 0 until 4) {
				val c = (cdata ushr n * 2) and 0b11
				bmp[pos + x] = RGBA.packRGB_A(cc[c], 0xFF)
				n++
			}
			pos += bmpStride
		}
	}
}

open class DXT2_3(format: String, premultiplied: Boolean) : DXT(format, premultiplied = premultiplied, blockSize = 16) {
	override fun decodeRow(data: ByteArray, dataOffset: Int, bmp: IntArray, bmpOffset: Int, bmpStride: Int) {
		decodeDxt5Alpha(data, dataOffset + 0, aa)
		decodeDxt1Color(data, dataOffset + 8, cc)
		val cdata = data.readS32_le(dataOffset + 8 + 4)
		val adata = data.readU32_le(dataOffset + 2) or (data.readU16_le(dataOffset + 6).toLong() shl 32)
		var pos = bmpOffset
		var n = 0
		for (y in 0 until 4) {
			for (x in 0 until 4) {
				val c = (cdata ushr n * 2) and 0b11
				val a = ((adata ushr n * 3) and 0b111).toInt()
				bmp[pos + x] = RGBA.packRGB_A(cc[c], aa[a])
				n++
			}
			pos += bmpStride
		}
	}
}

open class DXT4_5(format: String, premultiplied: Boolean) : DXT(format, premultiplied, blockSize = 16) {
	override fun decodeRow(data: ByteArray, dataOffset: Int, bmp: IntArray, bmpOffset: Int, bmpStride: Int) {
		decodeDxt5Alpha(data, dataOffset + 0, aa)
		decodeDxt1ColorCond(data, dataOffset + 8, cc)
		val cdata = data.readS32_le(dataOffset + 8 + 4)
		val adata = data.readU32_le(dataOffset + 2) or (data.readU16_le(dataOffset + 6).toLong() shl 32)
		var pos = bmpOffset
		var n = 0
		for (y in 0 until 4) {
			for (x in 0 until 4) {
				val c = (cdata ushr n * 2) and 0b11
				val a = ((adata ushr n * 3) and 0b111).toInt()
				bmp[pos + x] = RGBA.packRGB_A(cc[c], aa[a])
				n++
			}
			pos += bmpStride
		}
	}
}

abstract class DXT(val format: String, val premultiplied: Boolean, val blockSize: Int) : ImageFormat(format) {
	val aa = IntArray(8)
	val cc = IntArray(4)

	abstract fun decodeRow(data: ByteArray, dataOffset: Int, bmp: IntArray, bmpOffset: Int, bmpStride: Int)

	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		if (!PathInfo(props.filename).extensionLC.startsWith(format)) return null
		return ImageInfo().apply {
			width = props.width ?: 1
			height = props.height ?: 1
		}
	}

	final override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val bytes = s.readAll()
		val totalPixels = (bytes.size / blockSize) * 4 * 4
		val potentialSide = Math.sqrt(totalPixels.toDouble()).toInt()
		val width = props.width ?: potentialSide
		val height = props.height ?: potentialSide
		val out = Bitmap32(width, height, premultiplied = premultiplied)
		val blockWidth = out.width / 4
		val blockHeight = out.height / 4
		var offset = 0
		for (y in 0 until blockHeight) {
			for (x in 0 until blockWidth) {
				decodeRow(bytes, offset, out.data, out.index(x * 4, y * 4), out.width)
				offset += blockSize
			}
		}
		return ImageData(listOf(ImageFrame(out)))
	}

	companion object {
		fun decodeRGB656(v: Int): Int {
			//val b = v.extractScaledFF(0, 5)
			//val g = v.extractScaledFF(5, 6)
			//val r = v.extractScaledFF(11, 5)
			//return RGBA(r, g, b, 0xFF)
			return BGR_565.toRGBA(v)
		}

		fun decodeDxt1ColorCond(data: ByteArray, dataOffset: Int, cc: IntArray) {
			val c0 = data.readU16_le(dataOffset + 0)
			val c1 = data.readU16_le(dataOffset + 2)

			cc[0] = decodeRGB656(c0)
			cc[1] = decodeRGB656(c1)
			if (c0 > c1) {
				cc[2] = RGBA.blendRGB(cc[0], cc[1], 2.0 / 3.0)
				cc[3] = RGBA.blendRGB(cc[0], cc[1], 1.0 / 3.0)
			} else {
				cc[2] = RGBA.blendRGB(cc[0], cc[1], 1.0 / 2.0)
				cc[3] = Colors.TRANSPARENT_BLACK
			}
		}

		fun decodeDxt1Color(data: ByteArray, dataOffset: Int, cc: IntArray) {
			cc[0] = decodeRGB656(data.readU16_le(dataOffset + 0))
			cc[1] = decodeRGB656(data.readU16_le(dataOffset + 2))
			cc[2] = RGBA.blendRGB(cc[0], cc[1], 2.0 / 3.0)
			cc[3] = RGBA.blendRGB(cc[0], cc[1], 1.0 / 3.0)
		}

		fun decodeDxt5Alpha(data: ByteArray, dataOffset: Int, aa: IntArray) {
			val a0 = data.readU8(dataOffset + 0)
			val a1 = data.readU8(dataOffset + 1)
			aa[0] = a0
			aa[1] = a1
			aa[2] = ((6 * a0) + (1 * a1)) / 7
			aa[3] = ((5 * a0) + (2 * a1)) / 7
			aa[4] = ((4 * a0) + (3 * a1)) / 7
			aa[5] = ((3 * a0) + (4 * a1)) / 7
			aa[6] = ((2 * a0) + (5 * a1)) / 7
			aa[7] = ((1 * a0) + (6 * a1)) / 7
		}
	}
}
