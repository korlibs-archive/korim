package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.vfs.VfsFile

suspend fun ImageFormat.decode(s: VfsFile, props: ImageDecodingProps = ImageDecodingProps()) = this.read(s.readAsSyncStream(), props.copy(filename = s.basename))
suspend fun ImageFormat.decode(s: AsyncStream, filename: String) = this.read(s.readAll(), ImageDecodingProps(filename))
suspend fun ImageFormat.decode(s: AsyncStream, props: ImageDecodingProps = ImageDecodingProps()) = this.read(s.readAll(), props)

val nativeImageFormatProviders: List<NativeImageFormatProvider> by lazy {
	listOf(NativeImageFormatProvider)
}

val nativeImageFormatProvider: NativeImageFormatProvider = NativeImageFormatProvider

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
suspend fun VfsFile.readImageData(props: ImageDecodingProps = ImageDecodingProps(), formats: ImageFormats = defaultImageFormats): ImageData = formats.readImage(this.readAsSyncStream(), props.copy(filename = this.basename))
suspend fun VfsFile.readBitmapListNoNative(): List<Bitmap> = this.readImageData().frames.map { it.bitmap }

suspend fun AsyncInputStream.readNativeImage(): NativeImage = decodeImageBytes(this.readAll())
suspend fun AsyncInputStream.readImageData(basename: String = "file.bin", formats: ImageFormats = defaultImageFormats): ImageData = formats.readImageInWorker(this.readAll().openSync(), ImageDecodingProps(basename))
suspend fun AsyncInputStream.readImageDataProps(props: ImageDecodingProps = ImageDecodingProps("file.bin"), formats: ImageFormats = defaultImageFormats): ImageData = formats.readImageInWorker(this.readAll().openSync(), props)
suspend fun AsyncInputStream.readBitmapListNoNative(): List<Bitmap> = this.readImageData().frames.map { it.bitmap }
suspend fun VfsFile.readBitmapInfo(props: ImageDecodingProps = ImageDecodingProps(), formats: ImageFormats = defaultImageFormats): ImageInfo? = formats.decodeHeader(this.readAsSyncStream(), props)
suspend fun VfsFile.readImageData(formats: ImageFormats = defaultImageFormats): ImageData = formats.readImage(this.readAsSyncStream(), ImageDecodingProps(this.basename))
suspend fun VfsFile.readBitmapListNoNative(formats: ImageFormats = defaultImageFormats): List<Bitmap> = this.readImageData(formats).frames.map { it.bitmap }

suspend fun AsyncInputStream.readBitmap(basename: String, formats: ImageFormats = defaultImageFormats): Bitmap {
	return readBitmap(ImageDecodingProps(basename), formats)
}

suspend fun AsyncInputStream.readBitmap(props: ImageDecodingProps = ImageDecodingProps("file.bin"), formats: ImageFormats = defaultImageFormats): Bitmap {
	val bytes = this.readAll()
	return try {
		if (nativeImageLoadingEnabled) decodeImageBytes(bytes) else formats.decodeInWorker(bytes, props)
	} catch (t: Throwable) {
		formats.decodeInWorker(bytes, props)
	}
}


suspend fun VfsFile.readBitmapInfo(formats: ImageFormats = defaultImageFormats): ImageInfo? = formats.decodeHeader(this.readAsSyncStream())

suspend fun VfsFile.readBitmapOptimized(): Bitmap {
	try {
		//return this.readSpecial<NativeImage>() // @TODO: Kotlin.JS BUG!
		return this.readSpecial(NativeImage::class)
	} catch (t: Throwable) {
		t.printStackTrace()
		return this.readBitmap()
	}
}

suspend fun VfsFile.readBitmap(props: ImageDecodingProps = ImageDecodingProps(), formats: ImageFormats = defaultImageFormats): Bitmap {
	val file = this
	val bytes = this.read()
	return try {
		if (nativeImageLoadingEnabled) decodeImageBytes(bytes) else formats.decodeInWorker(bytes, props.copy(filename = file.basename))
	} catch (t: Throwable) {
		formats.decodeInWorker(bytes, props.copy(filename = file.basename))
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

suspend fun VfsFile.readBitmapNoNative(props: ImageDecodingProps = ImageDecodingProps(), formats: ImageFormats = defaultImageFormats): Bitmap = formats.readImageInWorker(this.readAsSyncStream(), props).mainBitmap

suspend fun VfsFile.readBitmapNoNative(formats: ImageFormats = defaultImageFormats): Bitmap = formats.decodeInWorker(this.read(), this.basename)

suspend fun VfsFile.writeBitmap(bitmap: Bitmap, format: ImageFormat = defaultImageFormats, props: ImageEncodingProps = ImageEncodingProps()) {
	this.write(format.encodeInWorker(bitmap, props.copy(filename = this.basename)))
}
//suspend fun VfsFile.writeBitmap(bitmap: Bitmap, format: ImageFormat = defaultImageFormats, props: ImageEncodingProps = ImageEncodingProps()) {
//	this.write(format.encodeInWorker(bitmap, this.basename, props))
//}
