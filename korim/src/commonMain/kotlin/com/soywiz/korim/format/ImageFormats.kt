package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

class ImageFormats(formats: Iterable<ImageFormat>) : ImageFormat("") {
	val formats = formats.toSet()
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		for (format in formats) return try {
			format.decodeHeader(s.sliceStart(), props) ?: continue
		} catch (e: Throwable) {
			continue
		}
		return null
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		//val format = formats.firstOrNull { it.check(s.sliceStart(), props) }
		//println("--------------")
		//println("FORMATS: $formats, props=$props")
		for (format in formats) {
			if (format.check(s.sliceStart(), props)) {
				//println("FORMAT CHECK: $format")
				return format.readImage(s.sliceStart(), props)
			}
		}
		//if (format != null) return format.readImage(s.sliceStart(), props)
		throw UnsupportedOperationException(
			"Not suitable image format : MAGIC:" + s.sliceStart().readString(4, ASCII) +
					"(" + s.sliceStart().readBytes(4).hex + ") (" + s.sliceStart().readBytes(4).toString(ASCII) + ")"
		)
	}

	override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
		val ext = PathInfo(props.filename).extensionLC
		//println("filename: $filename")
		val format = formats.firstOrNull { ext in it.extensions }
				?: throw UnsupportedOperationException("Don't know how to generate file for extension '$ext' (supported extensions ${formats.flatMap { it.extensions }}) (props $props)")
		format.writeImage(image, s, props)
	}
}

operator fun ImageFormats.plus(format: ImageFormat) = ImageFormats(this.formats + format)
operator fun ImageFormats.plus(format: Iterable<ImageFormat>) = ImageFormats(this.formats + format)

@Suppress("unused")
suspend fun Bitmap.writeTo(
	file: VfsFile,
	formats: ImageFormat,
	props: ImageEncodingProps = ImageEncodingProps()
) = file.writeBytes(formats.encode(this, props.copy(filename = file.basename)))

// @TODO: kotlin-native bug: https://github.com/JetBrains/kotlin-native/issues/1770
//val defaultImageFormats = ImageFormats(StandardImageFormats)

val defaultImageFormats get() = ImageFormats(StandardImageFormats)

//val defaultImageFormats = ImageFormats()
