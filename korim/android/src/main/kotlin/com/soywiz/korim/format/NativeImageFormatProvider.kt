package com.soywiz.korim.format

import android.graphics.BitmapFactory
import com.soywiz.korim.android.AndroidNativeImage
import com.soywiz.korim.android.androidShowImage
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage

actual object NativeImageFormatProvider {
	actual suspend fun decode(data: ByteArray): NativeImage = AndroidNativeImage(BitmapFactory.decodeByteArray(data, 0, data.size))

	actual fun create(width: Int, height: Int): NativeImage {
		val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
		//bmp.setPixels()
		return AndroidNativeImage(bmp)
	}

	actual fun copy(bmp: Bitmap): NativeImage {
		TODO()
	}

	actual suspend fun display(bitmap: Bitmap): Unit {
		androidShowImage(bitmap)
	}

	actual fun mipmap(bmp: Bitmap, levels: Int): NativeImage {
		TODO()
	}

	actual fun mipmap(bmp: Bitmap): NativeImage {
		TODO()
	}

}