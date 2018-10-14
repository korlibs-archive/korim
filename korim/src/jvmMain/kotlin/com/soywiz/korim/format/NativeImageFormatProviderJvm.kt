package com.soywiz.korim.format

import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.file.std.*
import java.awt.image.*
import java.io.*
import kotlin.math.*

actual val nativeImageFormatProvider: NativeImageFormatProvider = AwtNativeImageFormatProvider

object AwtNativeImageFormatProvider : NativeImageFormatProvider() {
	init {
		// Try to detect junit and run then in headless mode
		if (Thread.currentThread()!!.stackTrace!!.contentDeepToString().contains("org.junit")) {
			System.setProperty("java.awt.headless", "true")
		}
	}

	override suspend fun decode(data: ByteArray): NativeImage = AwtNativeImage(awtReadImageInWorker(data))

	override suspend fun decode(vfs: Vfs, path: String): NativeImage {
		return when (vfs) {
			is LocalVfs -> {
				//println("LOCAL: AwtImageSpecialReader.readSpecial: $vfs, $path")
				AwtNativeImage(awtReadImageInWorker(File(path)))
			}
			else -> {
				//println("OTHER: AwtImageSpecialReader.readSpecial: $vfs, $path")
				AwtNativeImage(awtReadImageInWorker(vfs[path].readAll()))
			}
		}
	}

	override fun create(width: Int, height: Int): NativeImage =
		AwtNativeImage(BufferedImage(Math.max(width, 1), Math.max(height, 1), BufferedImage.TYPE_INT_ARGB_PRE))

	override fun copy(bmp: Bitmap): NativeImage = AwtNativeImage(bmp.toAwt())
	override suspend fun display(bitmap: Bitmap, kind: Int): Unit = awtShowImageAndWait(bitmap)
	override fun mipmap(bmp: Bitmap, levels: Int): NativeImage = bmp.toBMP32().mipmap(levels).ensureNative()

	//actual fun mipmap(bmp: Bitmap, levels: Int): NativeImage {
	//	var out = bmp.ensureNative()
	//	for (n in 0 until levels) out = mipmap(out)
	//	return out
	//}

	override fun mipmap(bmp: Bitmap): NativeImage {
		val out = NativeImage(ceil(bmp.width * 0.5).toInt(), ceil(bmp.height * 0.5).toInt())
		out.getContext2d(antialiasing = true).renderer.drawImage(bmp, 0, 0, out.width, out.height)
		return out
	}

	//override fun mipmap(bmp: Bitmap, levels: Int): NativeImage = mipmapInternal(bmp, levels)
	////override fun mipmap(bmp: Bitmap): NativeImage = mipmapInternal(bmp, 1)
//
	//private fun mipmapInternal(bmp: Bitmap, levels: Int): NativeImage {
	//	val temp = (bmp.ensureNative() as AwtNativeImage).awtImage.clone()
	//	val g = temp.createGraphics()
	//	g.setRenderingHints(mapOf(
	//		RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BILINEAR
	//	))
	//	var twidth = bmp.width
	//	var theight = bmp.height
	//	for (n in 0 until levels) {
	//		val swidth = twidth
	//		val sheight = theight
	//		twidth /= 2
	//		theight /= 2
	//		g.drawImage(
	//			temp,
	//			0, 0, twidth, theight,
	//			0, 0, swidth, sheight,
	//			null
	//		)
	//	}
//
	//	return AwtNativeImage(temp.clone(twidth, theight))
//
	//	/*
	//	val scale = Math.pow(2.0, levels.toDouble()).toInt()
	//	val newWidth = bmp.width / scale
	//	val newHeight = bmp.height / scale
	//	val out = NativeImage(newWidth, newHeight) as AwtNativeImage
	//	val g = out.awtImage.createGraphics()
	//	g.setRenderingHints(mapOf(
	//		RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BILINEAR
	//	))
	//	g.drawImage(
	//		awtImageToDraw,
	//		0, 0,
	//		newWidth,
	//		newHeight,
	//		null
	//	)
	//	return out
	//	*/
	//}
}