package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage

abstract class NativeImageFormatProvider {
	abstract suspend fun decode(data: ByteArray): NativeImage
	abstract fun create(width: Int, height: Int): NativeImage
	open suspend fun display(bitmap: Bitmap): Unit {
		println("Not implemented NativeImageFormatProvider.display: $bitmap")
	}
}