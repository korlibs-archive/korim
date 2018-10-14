package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
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

		return when (h.bitsPerPixel) {
			8 -> {
				val out = Bitmap8(h.width, h.height)
				for (n in 0 until 256) out.palette.array[n] = RGBA.packFast(s.readS32_le(), 0xFF)
				for (n in 0 until h.height) out.setRow(h.height - n - 1, s.readBytes(h.width))
				ImageData(listOf(ImageFrame(out)))
			}
			24, 32 -> {
				val bytesPerRow = h.width * h.bitsPerPixel / 8
				val out = Bitmap32(h.width, h.height)
				val row = ByteArray(bytesPerRow)
				val format = if (h.bitsPerPixel == 24) BGR else RGBA
				val padding = 4 - (bytesPerRow % 4)
				for (n in 0 until h.height) {
					val y = h.height - n - 1
					s.read(row)
					format.decode(row, 0, out.data, out.index(0, y), h.width)
					if (padding != 0) {
						s.skip(padding)
					}
				}
				ImageData(listOf(ImageFrame(out)))
			}
			else -> TODO("Unsupported bitsPerPixel=${h.bitsPerPixel}")
		}
	}
}