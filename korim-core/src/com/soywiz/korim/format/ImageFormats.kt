package com.soywiz.korim.format

import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readString
import com.soywiz.korio.stream.slice
import com.soywiz.korio.vfs.PathInfo
import java.util.*

object ImageFormats : ImageFormat("") {
	private val formats = ServiceLoader.load(ImageFormat::class.java).toList()

	override fun decodeHeader(s: SyncStream, filename: String): ImageInfo? {
		for (format in formats) return try {
			format.decodeHeader(s.slice(), filename) ?: continue
		} catch (e: Throwable) {
			continue
		}
		return null
	}

	override fun readFrames(s: SyncStream, filename: String): List<ImageFrame> {
		for (format in formats) if (format.check(s.slice(), filename)) return format.readFrames(s.slice(), filename)
		throw UnsupportedOperationException("Not suitable image format : MAGIC:" + s.slice().readString(4))
	}

	override fun writeFrames(frames: List<ImageFrame>, s: SyncStream, filename: String) {
		val ext = PathInfo(filename).extensionLC
		println("filename: $filename")
		val format = formats.firstOrNull { ext in it.extensions } ?: throw UnsupportedOperationException("Don't know how to generate file for extension '$ext'")
		format.writeFrames(frames, s, filename)
	}
}