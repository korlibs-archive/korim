package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.ensureNative

abstract class NativeImageFormatProvider {
	abstract suspend fun decode(data: ByteArray): NativeImage
	abstract fun create(width: Int, height: Int): NativeImage
	abstract fun copy(bmp: Bitmap): NativeImage
	open suspend fun display(bitmap: Bitmap): Unit {
		println("Not implemented NativeImageFormatProvider.display: $bitmap")
	}

	open fun mipmap(bmp: Bitmap, levels: Int): NativeImage {
		var out = bmp.ensureNative()
		for (n in 0 until levels) out = mipmap(out)
		return out
	}

	open fun mipmap(bmp: Bitmap): NativeImage {
		val out = NativeImage(Math.ceil(bmp.width * 0.5).toInt(), Math.ceil(bmp.height * 0.5).toInt())
		out.getContext2d(antialiasing = true).renderer.drawImage(bmp, 0, 0, out.width, out.height)
		return out
	}
}