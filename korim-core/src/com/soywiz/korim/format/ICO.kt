package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmap4
import com.soywiz.korim.bitmap.Bitmap8
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.stream.*

object ICO : ImageFormat() {
	override fun decodeHeader(s: SyncStream): ImageInfo? {
		if (s.readU16_le() != 0) return null
		if (s.readU16_le() != 1) return null
		val count = s.readU16_le()
		if (count >= 1000) return null
		return ImageInfo()
	}

	override fun readFrames(s: SyncStream): List<ImageFrame> {
		data class DirEntry(
			val width: Int, val height: Int,
			val colorCount: Int,
			val reserved: Int,
			val planes: Int,
			val bitCount: Int,
			val size: Int,
			val offset: Int
		)

		fun readDirEntry() = DirEntry(
			width = s.readU8(),
			height = s.readU8(),
			colorCount = s.readU8(),
			reserved = s.readU8(),
			planes = s.readU16_le(),
			bitCount = s.readU16_le(),
			size = s.readS32_le(),
			offset = s.readS32_le()
		)

		fun readBitmap(e: DirEntry, s: SyncStream): Bitmap {
			val tryPNGHead = s.slice().readS32_be().toLong() and 0xFFFFFFFFL
			if (tryPNGHead == 0x89_50_4E_47L) {
				return PNG.decode(s.slice())
			}
			//println("%08X".format(tryPNGHead))

			val headerSize = s.readS32_le()
			val width = s.readS32_le()
			val height = s.readS32_le()
			val planes = s.readS16_le()
			val bitCount = s.readS16_le()
			val compression = s.readS32_le()
			val imageSize = s.readS32_le()
			val pixelsXPerMeter = s.readS32_le()
			val pixelsYPerMeter = s.readS32_le()
			val clrUsed = s.readS32_le()
			val clrImportant = s.readS32_le()
			var palette = IntArray(0)
			if (compression != 0) throw UnsupportedOperationException("Not supported compressed .ico")
			if (bitCount <= 8) {
				val colors = if (clrUsed == 0) 1 shl bitCount else clrUsed
				palette = (0 until colors).map {
					val b = s.readU8()
					val g = s.readU8()
					val r = s.readU8()
					val reserved = s.readU8()
					RGBA(r, g, b, 0xFF)
				}.toIntArray()
			}

			val stride = (e.width * bitCount) / 8
			val data = s.readBytes(stride * e.height)
			val maskData = s.readBytes(e.width * e.height / 8)
			val bmp2: Bitmap

			if (bitCount == 4) {
				val bmp = Bitmap4(e.width, e.height, data, palette)
				bmp2 = bmp
			} else if (bitCount == 8) {
				val bmp = Bitmap8(e.width, e.height, data, palette)
				bmp2 = bmp
			} else if (bitCount == 32) {
				//val stride = (e.width * bitCount) / 8
				val bmp = Bitmap32(e.width, e.height)
				bmp2 = bmp
				var n = 0
				for (y in 0 until e.height) {
					for (x in 0 until e.width) {
						val b = data[n++].toInt() and 0xFF
						val g = data[n++].toInt() and 0xFF
						val r = data[n++].toInt() and 0xFF
						val a = data[n++].toInt() and 0xFF
						bmp[x, y] = RGBA(r, g, b, a)
					}
				}
			} else {
				throw UnsupportedOperationException("Unsupported bitCount: $bitCount")
			}

			return bmp2
		}

		val reserved = s.readU16_le()
		val type = s.readU16_le()
		val count = s.readU16_le()
		val entries = (0 until count).map { readDirEntry() }
		val bitmaps = arrayListOf<Bitmap>()
		for (e in entries) {
			bitmaps += readBitmap(e, s.sliceWithSize(e.offset.toLong(), e.size.toLong()))
		}
		return bitmaps.map { ImageFrame(it, main = false) }
	}
}
