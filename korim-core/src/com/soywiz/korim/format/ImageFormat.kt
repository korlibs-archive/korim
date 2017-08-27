package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.executeInWorkerSync
import com.soywiz.korio.error.ignoreErrors
import com.soywiz.korio.service.Services
import com.soywiz.korio.stream.MemorySyncStreamToByteArray
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.vfs.VfsFile
import java.io.File

abstract class ImageFormat(vararg exts: String) : Services.Impl() {
	val extensions = exts.map { it.toLowerCase().trim() }.toSet()
	open fun readImage(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): ImageData = TODO()
	open fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps = ImageEncodingProps("unknown")): Unit = throw UnsupportedOperationException()

	open fun decodeHeader(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): ImageInfo? = ignoreErrors(show = true) {
		val bmp = read(s, props)
		ImageInfo().apply {
			this.width = bmp.width
			this.height = bmp.height
			this.bitsPerPixel = bmp.bpp
		}
	}


	fun read(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): Bitmap = readImage(s, props).mainBitmap
	fun read(file: File, props: ImageDecodingProps = ImageDecodingProps()) = this.read(file.openSync(), props.copy(filename = file.name))
	fun read(s: ByteArray, props: ImageDecodingProps = ImageDecodingProps()): Bitmap = read(s.openSync(), props)

	fun check(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): Boolean = ignoreErrors(show = true) { decodeHeader(s, props) != null } ?: false

	fun decode(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()) = this.read(s, props)
	fun decode(file: File, props: ImageDecodingProps = ImageDecodingProps()) = this.read(file.openSync("r"), props.copy(filename = file.name))
	fun decode(s: ByteArray, props: ImageDecodingProps = ImageDecodingProps()): Bitmap = read(s.openSync(), props)

	fun encode(frames: List<ImageFrame>, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray = MemorySyncStreamToByteArray(frames.area * 4) { writeImage(ImageData(frames), this, props) }
	fun encode(image: ImageData, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray = MemorySyncStreamToByteArray(image.area * 4) { writeImage(image, this, props) }
	fun encode(bitmap: Bitmap, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray = encode(listOf(ImageFrame(bitmap)), props)

	suspend fun read(file: VfsFile, props: ImageDecodingProps = ImageDecodingProps()) = this.readImageInWorker(file.readAll().openSync(), props.copy(filename = file.basename))
	suspend fun readImageInWorker(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): ImageData = executeInWorkerSync { readImage(s, props) }
	suspend fun decodeInWorker(s: ByteArray, props: ImageDecodingProps = ImageDecodingProps()): Bitmap = executeInWorkerSync { read(s.openSync(), props) }
	suspend fun encodeInWorker(bitmap: Bitmap, props: ImageEncodingProps = ImageEncodingProps()): ByteArray = executeInWorkerSync { encode(bitmap, props) }
}