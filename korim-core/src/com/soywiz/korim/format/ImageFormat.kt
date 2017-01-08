package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.stream.MemorySyncStreamToByteArray
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.openSync
import java.io.File

open class ImageFormat {
	open fun decodeHeader(s: SyncStream, filename: String = "unknown"): ImageInfo? = TODO()
	open fun readFrames(s: SyncStream, filename: String = "unknown"): List<ImageFrame> = TODO()
	fun read(s: SyncStream, filename: String = "unknown"): Bitmap = readFrames(s, filename).sortedByDescending {
		if (it.main) {
			Int.MAX_VALUE
		} else {
			it.bitmap.width * it.bitmap.height * (it.bitmap.bpp * it.bitmap.bpp)
		}
	}.firstOrNull()?.bitmap ?: throw IllegalArgumentException("No bitmap found")

	fun read(file: File) = this.read(file.openSync(), file.name)
	fun read(s: ByteArray, filename: String = "unknown"): Bitmap = read(s.openSync(), filename)
	open fun write(bitmap: Bitmap, s: SyncStream): Unit = TODO()

	fun check(s: SyncStream, filename: String): Boolean = decodeHeader(s, filename) != null

	fun decode(s: SyncStream, filename: String = "unknown") = this.read(s, filename)
	fun decode(file: File) = this.read(file.openSync("r"), file.name)
	fun decode(s: ByteArray, filename: String = "unknown"): Bitmap = read(s.openSync(), filename)

	fun encode(bitmap: Bitmap): ByteArray = MemorySyncStreamToByteArray { write(bitmap, this) }
}