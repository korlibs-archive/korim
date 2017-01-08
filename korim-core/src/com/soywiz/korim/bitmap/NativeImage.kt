package com.soywiz.korim.bitmap

abstract class NativeImage(width: Int, height: Int, val data: Any?) : Bitmap(width, height, 32) {
	abstract fun toNonNativeBmp(): Bitmap
	override fun swapRows(y0: Int, y1: Int) = throw UnsupportedOperationException()
	fun toBmp32(): Bitmap32 = toNonNativeBmp().toBMP32()
	override fun toString(): String = this.javaClass.simpleName + "($width, $height)"
}