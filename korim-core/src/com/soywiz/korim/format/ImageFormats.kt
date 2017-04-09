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

	override fun readImage(s: SyncStream, filename: String): ImageData {
		val format = formats.firstOrNull { it.check(s.slice(), filename) }
		if (format != null) return format.readImage(s.slice(), filename)
		throw UnsupportedOperationException("Not suitable image format : MAGIC:" + s.slice().readString(4))
	}

	override fun writeImage(image: ImageData, s: SyncStream, filename: String, props: ImageEncodingProps) {
		val ext = PathInfo(filename).extensionLC
		println("filename: $filename")
		val format = formats.firstOrNull { ext in it.extensions } ?: throw UnsupportedOperationException("Don't know how to generate file for extension '$ext'")
		format.writeImage(image, s, filename, props)
	}
}