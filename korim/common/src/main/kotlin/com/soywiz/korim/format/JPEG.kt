package com.soywiz.korim.format

import com.soywiz.korim.color.RGBA
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.stream.writeBytes

object JPEG : ImageFormat("jpg", "jpeg") {
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = try {
		val info = JPEGDecoder2.decodeInfo(s.readAll())
		ImageInfo().apply {
			this.width = info.width
			this.height = info.height
			this.bitsPerPixel = 24
		}
	} catch (e: Throwable) {
		null
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val data = JPEGDecoder2.decode(s.readAll())
		val out = RGBA.decodeToBitmap32(data.width, data.height, data.data.data)
		return ImageData(listOf(ImageFrame(out)))
	}

	override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
		val bmp = image.mainBitmap
		s.writeBytes(
			JPEGEncoder.encode(JPEGEncoder.ImageData(
				bmp.toBMP32().extractBytes(),
				bmp.width,
				bmp.height
			), qu = (props.quality * 100).toInt())
		)
	}
}
