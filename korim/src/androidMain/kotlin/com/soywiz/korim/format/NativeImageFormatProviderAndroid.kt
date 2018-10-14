package com.soywiz.korim.format

import android.graphics.BitmapFactory
import com.soywiz.korim.android.AndroidNativeImage
import com.soywiz.korim.android.androidShowImage
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage

object AndroidNativeImageFormatProvider : NativeImageFormatProvider() {
	override suspend fun decode(data: ByteArray): NativeImage =
		AndroidNativeImage(BitmapFactory.decodeByteArray(data, 0, data.size))

	override fun create(width: Int, height: Int): NativeImage {
		val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
		//bmp.setPixels()
		return AndroidNativeImage(bmp)
	}

	override fun copy(bmp: Bitmap): NativeImage {
		TODO()
	}

	override suspend fun display(bitmap: Bitmap): Unit {
		androidShowImage(bitmap)
	}

	override fun mipmap(bmp: Bitmap, levels: Int): NativeImage {
		TODO()
	}

	override fun mipmap(bmp: Bitmap): NativeImage {
		TODO()
	}

}

actual val nativeImageFormatProvider: NativeImageFormatProvider = AndroidNativeImageFormatProvider