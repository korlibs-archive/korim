package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmap8
import com.soywiz.korio.stream.*

@Suppress("UNUSED_VARIABLE")
object BMP : ImageFormat("bmp") {
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		if (s.readStringz(2) != "BM") return null
		// FILE HEADER
		val size = s.readS32_le()
		val reserved1 = s.readS16_le()
		val reserved2 = s.readS16_le()
		val offBits = s.readS32_le()
		// INFO HEADER
		val bsize = s.readS32_le()
		val width = s.readS32_le()
		val height = s.readS32_le()
		val planes = s.readS16_le()
		val bitcount = s.readS16_le()
		return ImageInfo().apply {
			this.width = width
			this.height = height
			this.bitsPerPixel = bitcount
		}
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val h = decodeHeader(s, props) ?: throw IllegalArgumentException("Not a BMP file")

		val compression = s.readS32_le()
		val sizeImage = s.readS32_le()
		val pixelsPerMeterX = s.readS32_le()
		val pixelsPerMeterY = s.readS32_le()
		val clrUsed = s.readS32_le()
		val clrImportant = s.readS32_le()

		if (h.bitsPerPixel == 8) {
			val out = Bitmap8(h.width, h.height)
			for (n in 0 until 256) out.palette[n] = s.readS32_le() or 0xFF000000.toInt()
			for (n in 0 until h.height) out.setRow(h.height - n - 1, s.readBytes(h.width))
			return ImageData(listOf(ImageFrame(out)))
		} else {
			val out = Bitmap32(h.width, h.height)
			for (n in 0 until h.height) out.setRow(h.height - n - 1, s.readIntArray_le(h.width))
			return ImageData(listOf(ImageFrame(out)))
		}
	}
}