package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.util.UByteArray
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

object JPEG : ImageFormat() {
	const val MAGIC = 0xFFD8

	override fun decodeHeader(s: SyncStream): ImageInfo? = try {
		val decoder = JPEGDecoder(ByteArrayInputStream(s.readAll()))
		decoder.decodeHeader()
		ImageInfo().apply {
			this.width = decoder.imageWidth
			this.height = decoder.imageHeight
			this.bitsPerPixel = 24
		}
	} catch (e: Throwable) {
		null
	}

	override fun read(s: SyncStream): Bitmap {
		val decoder = JPEGDecoder(ByteArrayInputStream(s.readAll()))
		decoder.decodeHeader()
		val width = decoder.imageWidth
		val height = decoder.imageHeight
		//val format = Texture.Format.RGBA;
		decoder.startDecode();
		val data = ByteArray(width * height * 4)
		val udata = UByteArray(data)
		val bb = ByteBuffer.wrap(data)
		decoder.decodeRGB(bb, width * 4, decoder.numMCURows)
		val out = Bitmap32(width, height)
		var n = 0
		for (y in 0 until height) {
			for (x in 0 until width) {
				val r = udata[n++]
				val g = udata[n++]
				val b = udata[n++]
				val a = udata[n++]
				out[x, y] = RGBA.packFast(r, g, b, a)
			}
		}
		return out
	}

	override fun write(bitmap: Bitmap, s: SyncStream) {
		super.write(bitmap, s)
	}
}