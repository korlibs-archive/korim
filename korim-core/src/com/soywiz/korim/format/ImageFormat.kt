package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.stream.MemorySyncStreamToByteArray
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.openSync
import java.io.File

open class ImageFormat {
	open fun decodeHeader(s: SyncStream): ImageInfo? = TODO()
	open fun readFrames(s: SyncStream): List<ImageFrame> = TODO()
	fun read(s: SyncStream): Bitmap = readFrames(s).sortedByDescending { it.bitmap.width * it.bitmap.height * (it.bitmap.bpp * it.bitmap.bpp) }.firstOrNull()?.bitmap ?: throw IllegalArgumentException("No bitmap found")

	fun read(file: File) = this.read(file.openSync())
	fun read(s: ByteArray): Bitmap = read(s.openSync())
	open fun write(bitmap: Bitmap, s: SyncStream): Unit = TODO()

	fun check(s: SyncStream): Boolean = decodeHeader(s) != null

	fun decode(s: SyncStream) = this.read(s)
	fun decode(file: File) = this.read(file.openSync("r"))
	fun decode(s: ByteArray): Bitmap = read(s.openSync())

	fun encode(bitmap: Bitmap): ByteArray = MemorySyncStreamToByteArray { write(bitmap, this) }
}