package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.stream.readS32_be
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

object JPEG : ImageFormat() {
	const val MAGIC = 0xFFD8FFE1.toInt()

	override fun check(s: SyncStream): Boolean {
		val magic = s.readS32_be()
		return magic == MAGIC
	}

	override fun read(s: SyncStream): Bitmap {
		val decoder = JPEGDecoder(ByteArrayInputStream(s.readAll()))
		decoder.decodeHeader()
		val width = decoder.imageWidth
		val height = decoder.imageHeight
		//val format = Texture.Format.RGBA;
		decoder.startDecode();
		val bb = ByteBuffer.allocate(width * height * 4)
		decoder.decodeRGB(bb, width * 4, decoder.numMCURows)
		val out = Bitmap32(width, height)
		var n = 0
		for (y in 0 until height) {
			for (x in 0 until width) {
				val b = bb[n++].toInt() and 0xFF
				val g = bb[n++].toInt() and 0xFF
				val r = bb[n++].toInt() and 0xFF
				val a = bb[n++].toInt() and 0xFF
				out[x, y] = RGBA.packFast(r, g, b, a)
			}
		}
		return out
	}

	override fun write(bitmap: Bitmap, s: SyncStream) {
		super.write(bitmap, s)
	}
}