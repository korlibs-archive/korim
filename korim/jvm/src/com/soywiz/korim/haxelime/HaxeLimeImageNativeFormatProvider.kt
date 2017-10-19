package com.soywiz.korim.haxelime

import com.jtransc.JTranscSystem
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.format.NativeImageFormatProvider

class HaxeLimeImageNativeFormatProvider : NativeImageFormatProvider() {
	override val available: Boolean = JTranscSystem.isHaxe()

	suspend override fun decode(data: ByteArray): NativeImage {
		TODO()
	}

	override fun create(width: Int, height: Int): NativeImage {
		return HaxeLimeNativeImage(width, height, null, false)
	}

	override fun copy(bmp: Bitmap): NativeImage {
		TODO()
	}
}