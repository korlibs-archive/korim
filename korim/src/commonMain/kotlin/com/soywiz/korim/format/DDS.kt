package com.soywiz.korim.format

import com.soywiz.kds.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*

object DDS : ImageFormat("dds") {
	var ImageInfo.fourcc by Extra.Property { "    " }

	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		if (s.readString(4) != "DDS ") return null
		val size = s.readS32_le()
		val sh = s.readStream(size - 4)
		val flags = sh.readS32_le()
		val height = sh.readS32_le()
		val width = sh.readS32_le()
		val pitchOrLinearSize = sh.readS32_le()
		val depth = sh.readS32_le()
		val mipmapCount = sh.readS32_le()
		val reserved = sh.readIntArray_le(11)

		val pf_size = sh.readS32_le()
		val pf_s = sh.readStream(pf_size - 4)
		val pf_flags = pf_s.readS32_le()
		val pf_fourcc = pf_s.readString(4)
		val pf_bitcount = pf_s.readS32_le()
		val pf_rbitmask = pf_s.readS32_le()
		val pf_gbitmask = pf_s.readS32_le()
		val pf_bbitmask = pf_s.readS32_le()
		val pf_abitmask = pf_s.readS32_le()

		val caps = sh.readS32_le()
		val caps2 = sh.readS32_le()
		val caps3 = sh.readS32_le()
		val caps4 = sh.readS32_le()

		val reserved2 = sh.readS32_le()

		return ImageInfo().apply {
			this.width = width
			this.height = height
			this.bitsPerPixel = 32
			this.fourcc = pf_fourcc
		}
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val h = decodeHeader(s, props) ?: invalidOp("Not a DDS file")
		val fourcc = h.fourcc.toUpperCase()
		val subimageFormat: DXT = when (fourcc) {
			"DXT1" -> DXT1
			"DXT3" -> DXT3
			"DXT4" -> DXT4
			"DXT5" -> DXT5
			else -> invalidOp("Unsupported DDS FourCC '$fourcc'")
		}
		val bytes = s.readAll()
		return subimageFormat.readImage(
			bytes.openSync(),
			ImageDecodingProps(filename = "image.$fourcc", width = h.width, height = h.height)
		)
	}
}