package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.stream.*
import com.soywiz.korim.format.ICO
import java.io.File

open class ImageFormat {
	open fun decodeHeader(s: SyncStream): ImageInfo? = TODO()
	open fun readBitmaps(s: SyncStream): List<Bitmap> = listOf(read(s))
	open fun read(s: SyncStream): Bitmap = TODO()
	fun read(file: File) = this.read(file.openSync())
	fun read(s: ByteArray): Bitmap = read(s.openSync())
	open fun write(bitmap: Bitmap, s: SyncStream): Unit = TODO()

	fun check(s: SyncStream): Boolean = decodeHeader(s) != null

	fun decode(s: SyncStream) = this.read(s)
	fun decode(file: File) = this.read(file.openSync("r"))
	fun decode(s: ByteArray): Bitmap = read(s.openSync())

	fun encode(bitmap: Bitmap): ByteArray = MemorySyncStreamToByteArray { write(bitmap, this) }
}

object ImageFormats : ImageFormat() {
	private val formats = listOf(PNG, JPEG, BMP, TGA, PSD, ICO)

	override fun decodeHeader(s: SyncStream): ImageInfo? {
		for (format in formats) return format.decodeHeader(s.slice()) ?: continue
		return null
	}

	override fun read(s: SyncStream): Bitmap {
		for (format in formats) if (format.check(s.slice())) return format.read(s.slice())
		throw UnsupportedOperationException("Not suitable image format : MAGIC:" + s.slice().readString(4))
	}
}

class ImageInfo {
	var width: Int = 0
	var height: Int = 0
	var bitsPerPixel: Int = 0
}