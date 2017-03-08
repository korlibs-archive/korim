package com.soywiz.korim.format

import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readS32_be
import com.soywiz.korio.stream.readStringz
import com.soywiz.korio.stream.readU16_be

// https://www.adobe.com/devnet-apps/photoshop/fileformatashtml/
class PSD : ImageFormat("psd") {
	override fun readImage(s: SyncStream, filename: String): Image {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun decodeHeader(s: SyncStream, filename: String): ImageInfo? {
		if (s.readStringz(4) != "8BPS") return null
		val version = s.readU16_be()
		when (version) {
			1 -> Unit
			2 -> return null // PSB file not supported yet!
			else -> return null
		}
		s.position += 6
		val channels = s.readU16_be()
		val height = s.readS32_be()
		val width = s.readS32_be()
		val bitsPerChannel = s.readU16_be()
		val colorMode = s.readU16_be()
		return ImageInfo().apply {
			this.width = width
			this.height = height
			this.bitsPerPixel = bitsPerPixel * channels
		}
	}
}