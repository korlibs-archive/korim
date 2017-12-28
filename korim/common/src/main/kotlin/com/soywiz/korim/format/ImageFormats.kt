package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.lang.ASCII
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readBytes
import com.soywiz.korio.stream.readString
import com.soywiz.korio.stream.slice
import com.soywiz.korio.util.hexString
import com.soywiz.korio.vfs.PathInfo
import com.soywiz.korio.vfs.VfsFile

class ImageFormats : ImageFormat("") {
	private val formats = linkedSetOf<ImageFormat>()

	fun clear() = this.apply { formats.clear() }
	fun backup(): List<ImageFormat> = formats.toList()
	fun restore(formats: List<ImageFormat>) {
		this.formats.clear()
		this.formats += formats
	}

	inline fun <T> saveRestore(callback: () -> T): T {
		val formats = backup()
		try {
			return callback()
		} finally {
			restore(formats)
		}
	}

	inline fun <T> temporalFormats(formats: Iterable<ImageFormat>, callback: () -> T): T = saveRestore {
		clear().register(formats)
		callback()
	}

	fun register(vararg format: ImageFormat): ImageFormats = this.apply { formats += format }
	fun register(format: ImageFormat): ImageFormats = this.apply { formats += format }
	fun register(format: Iterable<ImageFormat>): ImageFormats = this.apply { formats += format }

	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		for (format in formats) return try {
			format.decodeHeader(s.slice(), props) ?: continue
		} catch (e: Throwable) {
			continue
		}
		return null
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val format = formats.firstOrNull { it.check(s.slice(), props) }
		if (format != null) return format.readImage(s.slice(), props)
		throw UnsupportedOperationException("Not suitable image format : MAGIC:" + s.slice().readString(4, ASCII) + "(" + s.slice().readBytes(4).hexString + ") (" + s.slice().readBytes(4).toString(ASCII) + ")")
	}

	override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
		val ext = PathInfo(props.filename).extensionLC
		//println("filename: $filename")
		val format = formats.firstOrNull { ext in it.extensions } ?: throw UnsupportedOperationException("Don't know how to generate file for extension '$ext' (supported extensions ${formats.flatMap { it.extensions }}) (props $props)")
		format.writeImage(image, s, props)
	}
}

suspend fun Bitmap.writeTo(file: VfsFile, props: ImageEncodingProps = ImageEncodingProps(), formats: ImageFormat = defaultImageFormats) = file.writeBytes(formats.encode(this, props.copy(filename = file.basename)))

val defaultImageFormats by lazy { ImageFormats().registerStandard() }
//val defaultImageFormats = ImageFormats()
