package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import com.soywiz.korma.*
import kotlin.math.*

actual val nativeImageFormatProvider: NativeImageFormatProvider = NativeNativeImageFormatProvider

object NativeNativeImageFormatProvider : NativeImageFormatProvider() {
	override suspend fun decode(data: ByteArray): NativeImage = wrapNative(defaultImageFormats.decode(data))
	override suspend fun decode(vfs: Vfs, path: String): NativeImage = wrapNative(defaultImageFormats.decode(vfs[path]))
	private fun wrapNative(bmp: Bitmap): BitmapNativeImage {
		val bmp32: Bitmap32 = bmp.toBMP32()
		//bmp32.premultiplyInPlace()
		//return BitmapNativeImage(bmp32)
		return BitmapNativeImage(bmp32.premultiplied())
	}
	override fun create(width: Int, height: Int): NativeImage = BitmapNativeImage(Bitmap32(width, height))
	override fun copy(bmp: Bitmap): NativeImage = BitmapNativeImage(bmp)
	override suspend fun display(bitmap: Bitmap, kind: Int) {
		println("TODO: NativeNativeImageFormatProvider.display(bitmap=$bitmap, kind=$kind)")
	}
	override fun mipmap(bmp: Bitmap, levels: Int): NativeImage = BitmapNativeImage(bmp)
	override fun mipmap(bmp: Bitmap): NativeImage = BitmapNativeImage(bmp)
}

data class BitmapNativeImage(val bitmap: Bitmap32) : NativeImage(bitmap.width, bitmap.height, bitmap, bitmap.premult) {
	val intData: IntArray = bitmap.data.array

	constructor(bitmap: Bitmap) : this(bitmap.toBMP32())

	override fun getContext2d(antialiasing: Boolean): Context2d = bitmap.getContext2d(antialiasing)
	override fun toNonNativeBmp(): Bitmap = bitmap
}
