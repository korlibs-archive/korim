package com.soywiz.korim.bitmap

import com.soywiz.korim.format.nativeImageFormatProvider
import com.soywiz.korim.vector.Context2d

abstract class NativeImage(width: Int, height: Int, val data: Any?) : Bitmap(width, height, 32) {
	abstract fun toNonNativeBmp(): Bitmap
	override fun swapRows(y0: Int, y1: Int) = throw UnsupportedOperationException()
	fun toBmp32(): Bitmap32 = toNonNativeBmp().toBMP32()
	override fun toString(): String = this.javaClass.simpleName + "($width, $height)"
}

fun NativeImage(width: Int, height: Int) = nativeImageFormatProvider.create(width, height)

fun NativeImage(width: Int, height: Int, d: Context2d.Drawable, scaleX: Double = 1.0, scaleY: Double = scaleX): NativeImage {
	val bmp = NativeImage(width, height)
	val ctx = bmp.getContext2d()
	ctx.keep {
		ctx.scale(scaleX, scaleY)
		ctx.draw(d)
	}
	return bmp
}