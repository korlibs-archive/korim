package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.vfs.VfsFile
import java.util.*

suspend fun ImageFormat.decode(s: VfsFile) = asyncFun { this.read(s.readAsSyncStream(), s.basename) }
suspend fun ImageFormat.decode(s: AsyncStream, filename: String) = asyncFun { this.read(s.readAll(), filename) }

val nativeImageFormatProviders by lazy {
	ServiceLoader.load(NativeImageFormatProvider::class.java).toList()
}

val nativeImageFormatProvider by lazy { nativeImageFormatProviders.first() }

suspend fun decodeImageBytes(bytes: ByteArray): NativeImage = asyncFun {
	for (nip in nativeImageFormatProviders) {
		try {
			return@asyncFun nip.decode(bytes)
		} catch (t: Throwable) {
		}
	}
	throw UnsupportedOperationException("No format supported")
}

suspend fun VfsFile.readNativeImage(): NativeImage = asyncFun { decodeImageBytes(this.read()) }

suspend fun VfsFile.readImageFramesNoNative(): List<ImageFrame> = asyncFun {
	ImageFormats.readFrames(this.readAsSyncStream(), this.basename)
}

suspend fun VfsFile.readBitmapListNoNative(): List<Bitmap> = asyncFun {
	this.readImageFramesNoNative().map { it.bitmap }
}

suspend fun VfsFile.readBitmap(): Bitmap = asyncFun {
	val bytes = this.read()
	try {
		decodeImageBytes(bytes).toNonNativeBmp()
	} catch (t: Throwable) {
		ImageFormats.decode(bytes, this.basename)
	}
}

suspend fun VfsFile.readBitmapNoNative(): Bitmap = asyncFun { ImageFormats.decode(this.read(), this.basename) }

suspend fun VfsFile.writeBitmap(bitmap: Bitmap, format: ImageFormat = ImageFormats) = asyncFun {
	this.write(format.encode(bitmap, this.basename))
}
