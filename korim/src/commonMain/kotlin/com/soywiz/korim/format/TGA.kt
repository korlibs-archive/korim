package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.stream.*

object TGA : ImageFormat("tga") {
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		return try {
			val h = readHeader(s)
			ImageInfo().apply {
				this.width = h.width
				this.height = h.height
				this.bitsPerPixel = h.bitsPerPixel
			}
		} catch (t: Throwable) {
			null
		}
	}

	class Info(
		val width: Int,
		val height: Int,
		val flipY: Boolean,
		val bitsPerPixel: Int
	) {
		val area = width * height
		val bytes = bitsPerPixel / 8
	}

	// http://www.paulbourke.net/dataformats/tga/
	@Suppress("UNUSED_VARIABLE")
	fun readHeader(s: SyncStream): Info {
		val idLength = s.readU8()
		val colorMapType = s.readU8()
		val imageType = s.readU8()
		when (imageType) {
			1 -> TODO("Unsupported indexed")
			2 -> Unit // RGBA
			9, 10 -> TODO("Unsupported RLE")
			else -> TODO("Unknown TGA")
		}
		val firstIndexEntry = s.readU16_le()
		val colorMapLength = s.readU16_le()
		val colorMapEntrySize = s.readU8()
		s.position += colorMapLength * colorMapEntrySize
		val xorig = s.readS16_le()
		val yorig = s.readS16_le()
		val width = s.readS16_le()
		val height = s.readS16_le()
		val pixelDepth = s.readU8()
		when (pixelDepth) {
			24, 32 -> Unit
			else -> TODO("Not a RGBA tga")
		}
		val imageDescriptor = s.readU8()
		val flipY = ((imageDescriptor ushr 5) and 1) == 0
		val storage = ((imageDescriptor ushr 6) and 3)
		s.readBytes(idLength)
		return Info(width = width, height = height, flipY = flipY, bitsPerPixel = pixelDepth)
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val info = readHeader(s)
		val format = when (info.bitsPerPixel) {
			24 -> RGB
			32 -> RGBA
			else -> TODO("Not a RGBA tga")
		}
		val out = Bitmap32(info.width, info.height).writeDecoded(format, s.readBytes(info.area * info.bytes))
		if (info.flipY) out.flipY()
		return ImageData(listOf(ImageFrame(out)))
	}

	override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
		val bitmap = image.mainBitmap
		when (bitmap) {
			is Bitmap8 -> {
				TODO("Not implemented encoding TGA Bitmap8")
			}
			is Bitmap32 -> {
				val data = ByteArray(bitmap.area * 4)
				var m = 0
				for (c in bitmap.data) {
					data[m++] = c.b.toByte()
					data[m++] = c.g.toByte()
					data[m++] = c.r.toByte()
					data[m++] = c.a.toByte()
				}
				s.write8(0) // idLength
				s.write8(0) // colorMapType
				s.write8(2) // imageType=RGBA
				s.write16_le(0) // firstIndexEntry
				s.write16_le(0) // colorMapLength
				s.write8(0) // colorMapEntrySize
				s.write16_le(0) // xorig
				s.write16_le(0) // yorig
				s.write16_le(bitmap.width) // width
				s.write16_le(bitmap.height) // height
				s.write8(32) // pixelDepth
				s.write8(1 shl 5) // imageDescriptor
				//s.write8(0 shl 5) // imageDescriptor
				s.writeBytes(data)
			}
		}
	}
}