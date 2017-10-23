package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import kotlin.test.assertEquals

class ImageFormatsTest {
	val formats = ImageFormats().registerStandard().register(SVG).register(ICO)

	@kotlin.test.Test
	fun png8() = syncTest {
		val bitmap = ResourcesVfs["kotlin8.png"].readBitmapNoNative(formats)
		assertEquals("Bitmap8(190, 190, palette=32)", bitmap.toString())
	}

	@kotlin.test.Test
	fun png24() = syncTest {
		val bitmap = ResourcesVfs["kotlin24.png"].readBitmapNoNative(formats)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@kotlin.test.Test
	fun png32Encoder() = syncTest {
		val bitmap = ResourcesVfs["kotlin24.png"].readBitmapNoNative(formats)
		val data = PNG.encode(bitmap)
		val bitmap2 = PNG.decode(data)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
		assertEquals("Bitmap32(190, 190)", bitmap2.toString())
		assertEquals(true, Bitmap32.matches(bitmap, bitmap2))
	}

	@kotlin.test.Test
	fun png32EncoderPremultiplied() = syncTest {
		val bitmapOriginal = ResourcesVfs["kotlin32.png"].readBitmapNoNative(formats).toBMP32()
		val bitmap = bitmapOriginal.premultiplied()
		//showImageAndWait(bitmap)
		val data = PNG.encode(bitmap)
		val bitmap2 = PNG.decode(data)
		//showImageAndWait(bitmap2)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
		assertEquals("Bitmap32(190, 190)", bitmap2.toString())
		//showImageAndWait(Bitmap32.diff(bitmap, bitmap2))
		assertEquals(true, Bitmap32.matches(bitmapOriginal, bitmap2))
	}

	@kotlin.test.Test
	fun png32() = syncTest {
		val bitmap = ResourcesVfs["kotlin32.png"].readBitmapNoNative(formats)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@kotlin.test.Test
	fun tga() = syncTest {
		val bitmap = ResourcesVfs["kotlin.tga"].readBitmapNoNative(formats)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@kotlin.test.Test
	fun jpeg() = syncTest {
		val bitmap = ResourcesVfs["kotlin.jpg"].readBitmapNoNative(formats)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@kotlin.test.Test
	fun jpeg2() = syncTest {
		val bitmap = ResourcesVfs["img1.jpg"].readBitmapNoNative(formats)
		assertEquals("Bitmap32(460, 460)", bitmap.toString())
	}

	@kotlin.test.Test
	fun ico() = syncTest {
		val bitmaps = ResourcesVfs["icon.ico"].readBitmapListNoNative(formats)
		assertEquals(
			"[Bitmap32(256, 256), Bitmap32(128, 128), Bitmap32(96, 96), Bitmap32(72, 72), Bitmap32(64, 64), Bitmap32(48, 48), Bitmap32(32, 32), Bitmap32(24, 24), Bitmap32(16, 16)]",
			bitmaps.toString()
		)
	}

	//@Test
	////@Ignore
	//fun huge() = syncTest {
	//	//Thread.sleep(10000)
	//	val bitmap = Bitmap32(8196, 8196)
	//	//val bitmap = Bitmap32(32, 32)
	//	//val bitmap = Bitmap32(1, 1)
	//	val data = PNG().encode(bitmap, props = ImageEncodingProps(quality = 0.0))
	//	val bitmap2 = PNG().decode(data)
	//	assertEquals("Bitmap32(8196, 8196)", bitmap.toString())
	//	//assertEquals("Bitmap32(8196, 8196)", bitmap2.toString())
	//}
}