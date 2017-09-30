package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage

expect object NativeImageFormatProvider {
	suspend fun decode(data: ByteArray): NativeImage
	fun create(width: Int, height: Int): NativeImage
	fun copy(bmp: Bitmap): NativeImage
	suspend fun display(bitmap: Bitmap): Unit
	fun mipmap(bmp: Bitmap, levels: Int): NativeImage
	fun mipmap(bmp: Bitmap): NativeImage

	//open suspend fun display(bitmap: Bitmap): Unit {
	//	println("Not implemented NativeImageFormatProvider.display: $bitmap")
	//}
	//open fun mipmap(bmp: Bitmap, levels: Int): NativeImage {
	//	var out = bmp.ensureNative()
	//	for (n in 0 until levels) out = mipmap(out)
	//	return out
	//}
//
	//open fun mipmap(bmp: Bitmap): NativeImage {
	//	val out = NativeImage(ceil(bmp.width * 0.5).toInt(), ceil(bmp.height * 0.5).toInt())
	//	out.getContext2d(antialiasing = true).renderer.drawImage(bmp, 0, 0, out.width, out.height)
	//	return out
	//}
}