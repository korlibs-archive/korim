package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.stream.MemorySyncStreamToByteArray
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.openSync
import java.io.File

abstract class ImageFormat(vararg exts: String) {
	val extensions = exts.map { it.toLowerCase().trim() }.toSet()
	open fun readFrames(s: SyncStream, filename: String = "unknown"): List<ImageFrame> = TODO()
	open fun writeFrames(frames: List<ImageFrame>, s: SyncStream, filename: String = "unknown"): Unit = throw UnsupportedOperationException()
	open fun decodeHeader(s: SyncStream, filename: String = "unknown"): ImageInfo? = try {
		val bmp = read(s, filename)
		ImageInfo().apply {
			this.width = bmp.width
			this.height = bmp.height
			this.bitsPerPixel = bmp.bpp
		}
	} catch (e: Throwable) {
		null
	}

	fun read(s: SyncStream, filename: String = "unknown"): Bitmap = readFrames(s, filename).sortedByDescending {
		if (it.main) {
			Int.MAX_VALUE
		} else {
			it.bitmap.width * it.bitmap.height * (it.bitmap.bpp * it.bitmap.bpp)
		}
	}.firstOrNull()?.bitmap ?: throw IllegalArgumentException("No bitmap found")

	fun read(file: File) = this.read(file.openSync(), file.name)
	fun read(s: ByteArray, filename: String = "unknown"): Bitmap = read(s.openSync(), filename)

	fun check(s: SyncStream, filename: String): Boolean = try {
		decodeHeader(s, filename) != null
	} catch (e: Throwable) {
		false
	}

	fun decode(s: SyncStream, filename: String = "unknown") = this.read(s, filename)
	fun decode(file: File) = this.read(file.openSync("r"), file.name)
	fun decode(s: ByteArray, filename: String = "unknown"): Bitmap = read(s.openSync(), filename)

	fun encode(frames: List<ImageFrame>, filename: String = "unknown"): ByteArray = MemorySyncStreamToByteArray { writeFrames(frames, this, filename) }
	fun encode(bitmap: Bitmap, filename: String = "unknown"): ByteArray = encode(listOf(ImageFrame(bitmap)), filename)
}