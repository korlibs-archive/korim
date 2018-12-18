package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.stream.*

@Suppress("UNUSED_VARIABLE")
object ICO : ImageFormat("ico") {
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		if (s.readU16LE() != 0) return null
		if (s.readU16LE() != 1) return null
		val count = s.readU16LE()
		if (count >= 1000) return null
		return ImageInfo()
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
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
			planes = s.readU16LE(),
			bitCount = s.readU16LE(),
			size = s.readS32LE(),
			offset = s.readS32LE()
		)

		fun readBitmap(e: DirEntry, s: SyncStream): Bitmap {
			val tryPNGHead = s.sliceStart().readU32BE()
			if (tryPNGHead == 0x89_50_4E_47L) return PNG.decode(
				s.sliceStart(),
				props.copy(filename = "${props.filename}.png")
			)
			val headerSize = s.readS32LE()
			val width = s.readS32LE()
			val height = s.readS32LE()
			val planes = s.readS16LE()
			val bitCount = s.readS16LE()
			val compression = s.readS32LE()
			val imageSize = s.readS32LE()
			val pixelsXPerMeter = s.readS32LE()
			val pixelsYPerMeter = s.readS32LE()
			val clrUsed = s.readS32LE()
			val clrImportant = s.readS32LE()
			var palette = RgbaArray(0)
			if (compression != 0) throw UnsupportedOperationException("Not supported compressed .ico")
			if (bitCount <= 8) {
				val colors = if (clrUsed == 0) 1 shl bitCount else clrUsed
				palette = (0 until colors).map {
					val b = s.readU8()
					val g = s.readU8()
					val r = s.readU8()
					val reserved = s.readU8()
					RGBA(r, g, b, 0xFF)
				}.toRgbaArray()
			}

			val stride = (e.width * bitCount) / 8
			val data = s.readBytes(stride * e.height)

			return when (bitCount) {
				4 -> Bitmap4(e.width, e.height, data, palette)
				8 -> Bitmap8(e.width, e.height, data, palette)
				32 -> Bitmap32(e.width, e.height).writeDecoded(BGRA, data)
				else -> throw UnsupportedOperationException("Unsupported bitCount: $bitCount")
			}
		}

		val reserved = s.readU16LE()
		val type = s.readU16LE()
		val count = s.readU16LE()
		val entries = (0 until count).map { readDirEntry() }
		val bitmaps = arrayListOf<Bitmap>()
		for (e in entries) {
			val bmp = readBitmap(e, s.sliceWithSize(e.offset.toLong(), e.size.toLong()))
			bmp.flipY()
			bitmaps += bmp
		}
		return ImageData(bitmaps.map { ImageFrame(it, main = false) })
	}
}
