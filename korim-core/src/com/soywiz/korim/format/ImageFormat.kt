package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.error.ignoreErrors
import com.soywiz.korio.stream.MemorySyncStreamToByteArray
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.vfs.VfsFile
import java.io.File

abstract class ImageFormat(vararg exts: String) {
	val extensions = exts.map { it.toLowerCase().trim() }.toSet()
	open fun readImage(s: SyncStream, filename: String = "unknown"): ImageData = TODO()
	open fun writeImage(image: ImageData, s: SyncStream, filename: String = "unknown"): Unit = throw UnsupportedOperationException()
	open fun decodeHeader(s: SyncStream, filename: String = "unknown"): ImageInfo? = ignoreErrors {
		val bmp = read(s, filename)
		ImageInfo().apply {
			this.width = bmp.width
			this.height = bmp.height
			this.bitsPerPixel = bmp.bpp
		}
	}

	fun read(s: SyncStream, filename: String = "unknown"): Bitmap = readImage(s, filename).mainBitmap
	fun read(file: File) = this.read(file.openSync(), file.name)
	fun read(s: ByteArray, filename: String = "unknown"): Bitmap = read(s.openSync(), filename)
	fun check(s: SyncStream, filename: String): Boolean = ignoreErrors { decodeHeader(s, filename) != null } ?: false
	fun decode(s: SyncStream, filename: String = "unknown") = this.read(s, filename)
	fun decode(file: File) = this.read(file.openSync("r"), file.name)
	fun decode(s: ByteArray, filename: String = "unknown"): Bitmap = read(s.openSync(), filename)

	fun encode(frames: List<ImageFrame>, filename: String = "unknown"): ByteArray = MemorySyncStreamToByteArray { writeImage(ImageData(frames), this, filename) }
	fun encode(image: ImageData, filename: String = "unknown"): ByteArray = MemorySyncStreamToByteArray { writeImage(image, this, filename) }
	fun encode(bitmap: Bitmap, filename: String = "unknown"): ByteArray = encode(listOf(ImageFrame(bitmap)), filename)
}