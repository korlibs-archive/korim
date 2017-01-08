package com.soywiz.korim.format

import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readString
import com.soywiz.korio.stream.slice

object ImageFormats : ImageFormat() {
	private val formats = listOf(PNG, JPEG, BMP, TGA, PSD, ICO)

	override fun decodeHeader(s: SyncStream): ImageInfo? {
		for (format in formats) return format.decodeHeader(s.slice()) ?: continue
		return null
	}

	override fun readFrames(s: SyncStream): List<ImageFrame> {
		for (format in formats) if (format.check(s.slice())) return format.readFrames(s.slice())
		throw UnsupportedOperationException("Not suitable image format : MAGIC:" + s.slice().readString(4))
	}
}