package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

suspend fun ImageFormat.decode(s: VfsFile, props: ImageDecodingProps = ImageDecodingProps()) =
	this.read(s.readAsSyncStream(), props.copy(filename = s.baseName))

suspend fun ImageFormat.decode(s: AsyncStream, filename: String) = this.read(s.readAll(), ImageDecodingProps(filename))
suspend fun ImageFormat.decode(s: AsyncStream, props: ImageDecodingProps = ImageDecodingProps()) =
	this.read(s.readAll(), props)

val nativeImageFormatProviders: List<NativeImageFormatProvider> get() = listOf(nativeImageFormatProvider)

suspend fun displayImage(bmp: Bitmap, kind: Int = 0) = nativeImageFormatProvider.display(bmp, kind)

suspend fun decodeImageBytes(bytes: ByteArray): NativeImage {
	for (nip in nativeImageFormatProviders) {
		try {
			return nip.decode(bytes)
		} catch (t: Throwable) {
		}
	}
	throw UnsupportedOperationException("No format supported")
}

suspend fun decodeImageFile(file: VfsFile): NativeImage {
	for (nip in nativeImageFormatProviders) {
		try {
			return nip.decode(file.vfs, file.path)
		} catch (t: Throwable) {
		}
	}
	throw UnsupportedOperationException("No format supported")
}

suspend fun VfsFile.readNativeImage(): NativeImage = decodeImageFile(this)
suspend fun VfsFile.readImageData(formats: ImageFormat = RegisteredImageFormats, props: ImageDecodingProps = ImageDecodingProps()): ImageData =
	formats.readImage(this.readAsSyncStream(), props.copy(filename = this.baseName))


suspend fun AsyncInputStream.readNativeImage(): NativeImage = decodeImageBytes(this.readAll())
suspend fun AsyncInputStream.readImageData(formats: ImageFormat = RegisteredImageFormats, basename: String = "file.bin"): ImageData =
	formats.readImage(this.readAll().openSync(), ImageDecodingProps(basename))

suspend fun AsyncInputStream.readImageDataProps(
	formats: ImageFormat = RegisteredImageFormats, props: ImageDecodingProps = ImageDecodingProps("file.bin")
): ImageData = formats.readImage(this.readAll().openSync(), props)

suspend fun AsyncInputStream.readBitmapListNoNative(formats: ImageFormat): List<Bitmap> =
	this.readImageData(formats).frames.map { it.bitmap }

suspend fun VfsFile.readBitmapInfo(
	formats: ImageFormat = RegisteredImageFormats,
	props: ImageDecodingProps = ImageDecodingProps()
): ImageInfo? =
	formats.decodeHeader(this.readAsSyncStream(), props)

suspend fun VfsFile.readImageData(formats: ImageFormat): ImageData =
	formats.readImage(this.readAsSyncStream(), ImageDecodingProps(this.baseName))

suspend fun VfsFile.readBitmapListNoNative(formats: ImageFormat): List<Bitmap> =
	this.readImageData(formats).frames.map { it.bitmap }

suspend fun VfsFile.readBitmapImageData(formats: ImageFormat) = readImageData(formats)

suspend fun AsyncInputStream.readBitmap(basename: String, formats: ImageFormat): Bitmap {
	return readBitmap(formats, ImageDecodingProps(basename))
}

suspend fun AsyncInputStream.readBitmap(
	formats: ImageFormat = RegisteredImageFormats,
	props: ImageDecodingProps = ImageDecodingProps("file.bin")
): Bitmap {
	val bytes = this.readAll()
	return try {
		if (nativeImageLoadingEnabled) decodeImageBytes(bytes) else formats.decode(bytes, props)
	} catch (t: Throwable) {
		formats.decode(bytes, props)
	}
}


suspend fun VfsFile.readBitmapInfo(formats: ImageFormat): ImageInfo? =
	formats.decodeHeader(this.readAsSyncStream())

suspend fun VfsFile.readBitmapOptimized(formats: ImageFormat = RegisteredImageFormats, premultiplied: Boolean = true): Bitmap {
	try {
		return nativeImageFormatProvider.decode(this, premultiplied)
	} catch (t: Throwable) {
		t.printStackTrace()
		return this.readBitmap(formats)
	}
}

suspend fun VfsFile.readBitmap(
    formats: ImageFormat = RegisteredImageFormats,
    props: ImageDecodingProps = ImageDecodingProps()
): Bitmap = when {
    nativeImageLoadingEnabled -> {
        try {
            nativeImageFormatProvider.decode(this)
        } catch (e: Throwable) {
            println("Couldn't read native image: $e")
            e.printStackTrace()
            formats.decode(this.read(), props.copy(filename = this.baseName))
        }
    }
    else -> formats.decode(this.read(), props.copy(filename = this.baseName))
}

suspend fun VfsFile.readBitmapSlice(formats: ImageFormat = RegisteredImageFormats): BitmapSlice<Bitmap> = readBitmapOptimized().slice()

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

suspend fun VfsFile.readBitmapNoNative(
	formats: ImageFormat = RegisteredImageFormats,
	props: ImageDecodingProps = ImageDecodingProps()
): Bitmap = formats.readImage(this.readAsSyncStream(), props).mainBitmap

suspend fun VfsFile.readBitmapNoNative(formats: ImageFormat = RegisteredImageFormats): Bitmap =
	formats.decode(this, ImageDecodingProps(this.baseName))

suspend fun VfsFile.writeBitmap(
	bitmap: Bitmap,
	format: ImageFormat,
	props: ImageEncodingProps = ImageEncodingProps()
) {
	this.write(format.encode(bitmap, props.copy(filename = this.baseName)))
}
//suspend fun VfsFile.writeBitmap(bitmap: Bitmap, format: ImageFormat =
// defaultImageFormats, props: ImageEncodingProps = ImageEncodingProps()) {
//	this.write(format.encodeInWorker(bitmap, this.basename, props))
//}
