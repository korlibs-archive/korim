package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korio.service.Services
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.vfs.VfsFile

suspend fun ImageFormat.decode(s: VfsFile, props: ImageDecodingProps = ImageDecodingProps()) = this.read(s.readAsSyncStream(), props.copy(filename = s.basename))
suspend fun ImageFormat.decode(s: AsyncStream, filename: String) = this.read(s.readAll(), ImageDecodingProps(filename))
suspend fun ImageFormat.decode(s: AsyncStream, props: ImageDecodingProps = ImageDecodingProps()) = this.read(s.readAll(), props)

val nativeImageFormatProviders by lazy {
	Services.loadList(NativeImageFormatProvider::class.java)
}

val nativeImageFormatProvider by lazy { Services.load(NativeImageFormatProvider::class.java) }

suspend fun displayImage(bmp: Bitmap) = nativeImageFormatProvider.display(bmp)

suspend fun decodeImageBytes(bytes: ByteArray): NativeImage {
	for (nip in nativeImageFormatProviders) {
		try {
			return nip.decode(bytes)
		} catch (t: Throwable) {
		}
	}
	throw UnsupportedOperationException("No format supported")
}

suspend fun VfsFile.readNativeImage(): NativeImage = decodeImageBytes(this.read())
suspend fun VfsFile.readImageData(props: ImageDecodingProps = ImageDecodingProps()): ImageData = ImageFormats.readImage(this.readAsSyncStream(), props.copy(filename = this.basename))
suspend fun VfsFile.readBitmapListNoNative(): List<Bitmap> = this.readImageData().frames.map { it.bitmap }

suspend fun AsyncInputStream.readNativeImage(): NativeImage = decodeImageBytes(this.readAll())
suspend fun AsyncInputStream.readImageData(basename: String = "file.bin"): ImageData = ImageFormats.readImageInWorker(this.readAll().openSync(), ImageDecodingProps(basename))
suspend fun AsyncInputStream.readImageDataProps(props: ImageDecodingProps = ImageDecodingProps("file.bin")): ImageData = ImageFormats.readImageInWorker(this.readAll().openSync(), props)
suspend fun AsyncInputStream.readBitmapListNoNative(): List<Bitmap> = this.readImageData().frames.map { it.bitmap }
suspend fun AsyncInputStream.readBitmap(props: ImageDecodingProps = ImageDecodingProps("file.bin")): Bitmap {
	val bytes = this.readAll()
	return try {
		if (nativeImageLoadingEnabled) decodeImageBytes(bytes) else ImageFormats.decodeInWorker(bytes, props)
	} catch (t: Throwable) {
		ImageFormats.decodeInWorker(bytes, props)
	}
}

suspend fun VfsFile.readBitmapInfo(props: ImageDecodingProps = ImageDecodingProps()): ImageInfo? = ImageFormats.decodeHeader(this.readAsSyncStream(), props)

suspend fun VfsFile.readBitmapOptimized(): Bitmap {
	try {
		return this.readSpecial<NativeImage>()
	} catch (t: Throwable) {
		t.printStackTrace()
		return this.readBitmap()
	}
}

suspend fun VfsFile.readBitmap(props: ImageDecodingProps = ImageDecodingProps()): Bitmap {
	val file = this
	val bytes = this.read()
	return try {
		if (nativeImageLoadingEnabled) decodeImageBytes(bytes) else ImageFormats.decodeInWorker(bytes, props.copy(filename = file.basename))
	} catch (t: Throwable) {
		ImageFormats.decodeInWorker(bytes,  props.copy(filename = file.basename))
	}
}

var nativeImageLoadingEnabled = true

suspend inline fun disableNativeImageLoading(callback: () -> Unit) {
	val oldNativeImageLoadingEnabled = nativeImageLoadingEnabled
	try {
		nativeImageLoadingEnabled = false
		callback()
	} finally {
		nativeImageLoadingEnabled = oldNativeImageLoadingEnabled
	}
}

suspend fun VfsFile.readBitmapNoNative(props: ImageDecodingProps = ImageDecodingProps()): Bitmap = ImageFormats.readImageInWorker(this.readAsSyncStream(), props).mainBitmap

suspend fun VfsFile.writeBitmap(bitmap: Bitmap, format: ImageFormat = ImageFormats, props: ImageEncodingProps = ImageEncodingProps()) {
	this.write(format.encodeInWorker(bitmap, props.copy(filename = this.basename)))
}
