package com.soywiz.korim.format

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.stream.*
import kotlin.math.*

// https://en.wikipedia.org/wiki/S3_Texture_Compression
object DXT1 : DXT1Base("dxt1", premult = true)

object DXT2 : DXT2_3("dxt2", premult = true)
object DXT3 : DXT2_3("dxt3", premult = false)
object DXT4 : DXT4_5("dxt4", premult = true)
object DXT5 : DXT4_5("dxt5", premult = false)

open class DXT1Base(format: String, premult: Boolean) : DXT(format, premult = true, blockSize = 8) {
	override fun decodeRow(data: ByteArray, dataOffset: Int, bmp: RgbaArray, bmpOffset: Int, bmpStride: Int, aa: IntArray, cc: RgbaArray) {
		decodeDxt1ColorCond(data, dataOffset + 0, cc)
		val cdata = data.readS32_le(dataOffset + 4)
		var pos = bmpOffset
		var n = 0
		for (y in 0 until 4) {
			for (x in 0 until 4) {
				val c = (cdata ushr n * 2) and 0b11
				bmp.array[pos + x] = RGBA.packFast(RGBA.getRGB(cc.array[c]), 0xFF)
				n++
			}
			pos += bmpStride
		}
	}
}

open class DXT2_3(format: String, premult: Boolean) : DXT(format, premult = premult, blockSize = 16) {
	override fun decodeRow(data: ByteArray, dataOffset: Int, bmp: RgbaArray, bmpOffset: Int, bmpStride: Int, aa: IntArray, cc: RgbaArray) {
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
				bmp.array[pos + x] = RGBA.packFast(RGBA.getRGB(cc.array[c]), aa[a])
				n++
			}
			pos += bmpStride
		}
	}
}

open class DXT4_5(format: String, premult: Boolean) : DXT(format, premult, blockSize = 16) {
	override fun decodeRow(data: ByteArray, dataOffset: Int, bmp: RgbaArray, bmpOffset: Int, bmpStride: Int, aa: IntArray, cc: RgbaArray) {
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
				bmp.array[pos + x] = RGBA.packFast(RGBA.getRGB(cc.array[c]), aa[a])
				n++
			}
			pos += bmpStride
		}
	}
}

abstract class DXT(val format: String, val premult: Boolean, val blockSize: Int) : ImageFormat(format) {
	abstract fun decodeRow(data: ByteArray, dataOffset: Int, bmp: RgbaArray, bmpOffset: Int, bmpStride: Int, aa: IntArray, cc: RgbaArray)

	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		if (!PathInfo(props.filename).extensionLC.startsWith(format)) return null
		return ImageInfo().apply {
			width = props.width ?: 1
			height = props.height ?: 1
		}
	}

	fun decodeBitmap(bytes: ByteArray, width: Int, height: Int): Bitmap32 {
		val out = Bitmap32(width, height, premult = premult)
		val blockWidth = out.width / 4
		val blockHeight = out.height / 4
		var offset = 0

		val aa = IntArray(8)
		val cc = RgbaArray(4)

		for (y in 0 until blockHeight) {
			for (x in 0 until blockWidth) {
				decodeRow(bytes, offset, out.data, out.index(x * 4, y * 4), out.width, aa, cc)
				offset += blockSize
			}
		}
		return out
	}

	final override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val bytes = s.readAll()
		val totalPixels = (bytes.size / blockSize) * 4 * 4
		val potentialSide = sqrt(totalPixels.toDouble()).toInt()
		val width = props.width ?: potentialSide
		val height = props.height ?: potentialSide
		return ImageData(listOf(ImageFrame(decodeBitmap(bytes, width, height))))
	}

	companion object {
		fun decodeRGB656(v: Int): RGBA = BGR_565.toRGBA(v)
		fun decodeRGB656Int(v: Int): Int = BGR_565.toRGBAInt(v)

		//fun blendComponent(l: Int, r: Int, num: Int, den: Int): Int {
		//	return l + ((r - l) * num / den)
		//}
		//
		//fun blendRGBA(l: Int, r: Int, num: Int, den: Int): Int {
		//	return RGBA.packFast(
		//			blendComponent(RGBA.getFastR(l), RGBA.getFastR(r), num, den),
		//			blendComponent(RGBA.getFastG(l), RGBA.getFastG(r), num, den),
		//			blendComponent(RGBA.getFastB(l), RGBA.getFastB(r), num, den),
		//			blendComponent(RGBA.getFastA(l), RGBA.getFastA(r), num, den)
		//	)
		//}

		const val FACT_2_3: Int = ((2.0 / 3.0) * 256).toInt()
		const val FACT_1_3: Int = ((1.0 / 3.0) * 256).toInt()
		const val FACT_1_2: Int = ((1.0 / 2.0) * 256).toInt()

		fun decodeDxt1ColorCond(data: ByteArray, dataOffset: Int, cc: RgbaArray) {
			val c0 = data.readU16_le(dataOffset + 0)
			val c1 = data.readU16_le(dataOffset + 2)
			val ccArray = cc.array

			ccArray[0] = decodeRGB656Int(c0)
			ccArray[1] = decodeRGB656Int(c1)
			if (c0 > c1) {
				ccArray[2] = RGBA.blendRGB(cc.array[0], cc.array[1], FACT_2_3)
				ccArray[3] = RGBA.blendRGB(cc.array[0], cc.array[1], FACT_1_3)
				//cc[2] = blendRGBA(cc[0], cc[1], 2, 3)
				//cc[3] = blendRGBA(cc[0], cc[1], 1, 3)
			} else {
				ccArray[2] = RGBA.blendRGB(cc.array[0], cc.array[1], FACT_1_2)
				//cc[2] = blendRGBA(cc[0], cc[1], 1, 2)
				ccArray[3] = Colors.TRANSPARENT_BLACK.rgba
			}
		}

		fun decodeDxt1Color(data: ByteArray, dataOffset: Int, cc: RgbaArray) {
			cc.array[0] = decodeRGB656Int(data.readU16_le(dataOffset + 0))
			cc.array[1] = decodeRGB656Int(data.readU16_le(dataOffset + 2))
			cc.array[2] = RGBA.blendRGB(cc.array[0], cc.array[1], FACT_2_3)
			cc.array[3] = RGBA.blendRGB(cc.array[0], cc.array[1], FACT_1_3)
			//cc[2] = blendRGBA(cc[0], cc[1], 2, 3)
			//cc[3] = blendRGBA(cc[0], cc[1], 1, 3)
		}

		fun decodeDxt5Alpha(data: ByteArray, dataOffset: Int, aa: IntArray) {
			val a0 = data.readU8(dataOffset + 0)
			val a1 = data.readU8(dataOffset + 1)
			aa[0] = a0
			aa[1] = a1
			if (a0 > a1) {
				aa[2] = ((6 * a0) + (1 * a1)) / 7
				aa[3] = ((5 * a0) + (2 * a1)) / 7
				aa[4] = ((4 * a0) + (3 * a1)) / 7
				aa[5] = ((3 * a0) + (4 * a1)) / 7
				aa[6] = ((2 * a0) + (5 * a1)) / 7
				aa[7] = ((1 * a0) + (6 * a1)) / 7
			} else {
				aa[2] = ((4 * a0) + (1 * a1)) / 5
				aa[3] = ((3 * a0) + (2 * a1)) / 5
				aa[4] = ((2 * a0) + (3 * a1)) / 5
				aa[5] = ((1 * a0) + (4 * a1)) / 5
				aa[6] = 0x00
				aa[7] = 0xFF
			}
		}
	}
}
