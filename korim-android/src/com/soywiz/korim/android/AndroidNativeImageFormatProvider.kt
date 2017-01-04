package com.soywiz.korim.android

import android.graphics.BitmapFactory
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.format.NativeImageFormatProvider

fun Bitmap.toAndroidBitmap(): android.graphics.Bitmap {
	val bmp32 = this.toBMP32()
	return android.graphics.Bitmap.createBitmap(bmp32.data, 0, bmp32.width, bmp32.width, bmp32.height, android.graphics.Bitmap.Config.ARGB_8888)
}

class AndroidNativeImage(val androidBitmap: android.graphics.Bitmap) : NativeImage(androidBitmap.width, androidBitmap.height, androidBitmap) {
	override fun toNonNativeBmp(): Bitmap {
		val out = IntArray(width * height)
		androidBitmap.getPixels(out, 0, width, 0, 0, width, height)
		return Bitmap32(width, height, out)
	}
}

class AndroidNativeImageFormatProvider : NativeImageFormatProvider() {
	suspend override fun decode(data: ByteArray): NativeImage = AndroidNativeImage(BitmapFactory.decodeByteArray(data, 0, data.size))
}