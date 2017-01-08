package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.vfs.VfsFile
import java.util.*

suspend fun ImageFormat.decode(s: VfsFile) = asyncFun { this.read(s.readAsSyncStream()) }
suspend fun ImageFormat.decode(s: AsyncStream) = asyncFun { this.read(s.readAll()) }

val nativeImageFormatProviders by lazy {
	ServiceLoader.load(NativeImageFormatProvider::class.java).toList()
}

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
	ImageFormats.readFrames(this.readAsSyncStream())
}

suspend fun VfsFile.readBitmapListNoNative(): List<Bitmap> = asyncFun {
	this.readImageFramesNoNative().map { it.bitmap }
}

suspend fun VfsFile.readBitmap(): Bitmap = asyncFun {
	val bytes = this.read()
	try {
		decodeImageBytes(bytes).toNonNativeBmp()
	} catch (t: Throwable) {
		ImageFormats.decode(bytes)
	}
}

suspend fun VfsFile.readBitmapNoNative(): Bitmap = asyncFun { ImageFormats.decode(this.read()) }
