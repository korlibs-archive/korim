package com.soywiz.korim.bitmap

import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.lang.*

abstract class NativeImage(width: Int, height: Int, val data: Any?, premultiplied: Boolean) :
	Bitmap(width, height, 32, premultiplied, null) {
	open val name: String = "NativeImage"
	abstract fun toNonNativeBmp(): Bitmap
	override fun swapRows(y0: Int, y1: Int) = throw UnsupportedOperationException()
	fun toBmp32(): Bitmap32 = toNonNativeBmp().toBMP32()
	open fun toUri(): String = "data:image/png;base64," + Base64.encode(PNG.encode(this, ImageEncodingProps("out.png")))
	override fun createWithThisFormat(width: Int, height: Int): Bitmap = NativeImage(width, height)

	override fun toString(): String = "$name($width, $height)"
}

fun Bitmap.mipmap(levels: Int): NativeImage = nativeImageFormatProvider.mipmap(this, levels)

fun Bitmap.toUri(): String {
	if (this is NativeImage) return this.toUri()
	return "data:image/png;base64," + Base64.encode(PNG.encode(this, ImageEncodingProps("out.png")))
}

fun NativeImage(width: Int, height: Int) = nativeImageFormatProvider.create(width, height)

fun NativeImage(
	width: Int,
	height: Int,
	d: Context2d.Drawable,
	scaleX: Double = 1.0,
	scaleY: Double = scaleX
): NativeImage {
	val bmp = NativeImage(width, height)
	try {
		val ctx = bmp.getContext2d()
		ctx.keep {
			ctx.scale(scaleX, scaleY)
			ctx.draw(d)
		}
	} catch (e: Throwable) {
		e.printStackTrace()
	}
	return bmp
}

fun NativeImage(d: Context2d.SizedDrawable, scaleX: Double = 1.0, scaleY: Double = scaleX): NativeImage {
	return NativeImage((d.width * scaleX).toInt(), (d.height * scaleY).toInt(), d, scaleX, scaleY)
}

fun Bitmap.ensureNative() = when (this) {
	is NativeImage -> this
	else -> nativeImageFormatProvider.copy(this)
}

fun Context2d.SizedDrawable.raster(scaleX: Double = 1.0, scaleY: Double = scaleX) = NativeImage(this, scaleX, scaleY)