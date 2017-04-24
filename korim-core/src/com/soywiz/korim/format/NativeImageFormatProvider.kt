package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage

abstract class NativeImageFormatProvider {
	abstract suspend fun decode(data: ByteArray): NativeImage
	abstract fun create(width: Int, height: Int): NativeImage
	abstract fun copy(bmp: Bitmap): NativeImage
	open suspend fun display(bitmap: Bitmap): Unit {
		println("Not implemented NativeImageFormatProvider.display: $bitmap")
	}

	open fun scaled(bmp: Bitmap, scale: Double): NativeImage {
		val out = NativeImage(Math.ceil(bmp.width * scale).toInt(), Math.ceil(bmp.height * scale).toInt())
		out.getContext2d(antialiasing = true).renderer.drawImage(bmp, 0, 0, out.width, out.height)
		return out
	}
}