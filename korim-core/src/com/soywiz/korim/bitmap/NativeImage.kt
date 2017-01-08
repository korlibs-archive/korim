package com.soywiz.korim.bitmap

abstract class NativeImage(width: Int, height: Int, val data: Any?) : Bitmap(width, height) {
	override val bpp = 32
	abstract fun toNonNativeBmp(): Bitmap
	fun toBmp32(): Bitmap32 = toNonNativeBmp().toBMP32()
}