package com.soywiz.korim.haxelime

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage

class HaxeLimeNativeImage(width: Int, height: Int, data: Any?, premultiplied: Boolean) : NativeImage(width, height, data, premultiplied) {
	override fun toNonNativeBmp(): Bitmap {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}