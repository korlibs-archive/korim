package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import kotlin.test.*

class ImageFormatsTest : BaseImageFormatTest() {
	val imageFormats = ImageFormats(StandardImageFormats + SVG + ICO + BMP)

	//@Test
	//fun demo1() = imageTest {
	//  val tempVfs = LocalVfs("c:/temp/")
	//	tempVfs["1.png"].readBitmap().writeTo(tempVfs["1.out.png"])
	//	Bitmap32(32, 32, premult = true) { x, y -> if ((x + y) % 2 == 0) Colors.RED else Colors.BLUE }.writeTo(tempVfs["red.png"])
	//	//println("ResourcesVfs.absolutePath:" + ResourcesVfs.absolutePath)
	//}


	@Test
	fun png8() = suspendTest {
		//println("ResourcesVfs.absolutePath:" + ResourcesVfs.absolutePath)
		val bitmap = root["kotlin8.png"].readBitmapNoNative(imageFormats)
		assertEquals("Bitmap8(190, 190, palette=32)", bitmap.toString())
	}

	@Test
	fun png24() = suspendTest {
		val bitmap = root["kotlin24.png"].readBitmapNoNative(imageFormats)
		//JailedLocalVfs("c:/temp/")["lol.png"].writeBitmap(bitmap, formats)
		//root["kotlin8.png"].writeBitmap()
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun bmp() = suspendTest {
		val bitmap = root["kotlin.bmp"].readBitmapNoNative(imageFormats)
		//JailedLocalVfs("c:/temp/")["lol.png"].writeBitmap(bitmap, formats)
		//root["kotlin8.png"].writeBitmap()
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
		//showImageAndWait(bitmap)
	}

	@Test
	fun png32Encoder() = suspendTest {
		val bitmap = root["kotlin24.png"].readBitmapNoNative(imageFormats)
		val data = PNG.encode(bitmap)
		val bitmap2 = PNG.decode(data)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
		assertEquals("Bitmap32(190, 190)", bitmap2.toString())
		assertEquals(true, Bitmap32.matches(bitmap, bitmap2))
	}

	@Test
	fun png32EncoderPremultiplied() = suspendTest {
		val bitmapOriginal = root["kotlin32.png"].readBitmapNoNative(imageFormats).toBMP32()
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

	@Test
	fun jpegEncoder() = suspendTest {
		val bitmapOriginal = root["kotlin32.png"].readBitmapNoNative(imageFormats).toBMP32()
		val bytes = JPEG.encode(bitmapOriginal, ImageEncodingProps(quality = 0.5))
		//val bitmapOriginal = LocalVfs("/tmp/aa.jpg").readBitmapNoNative().toBMP32()
		//bitmapOriginal.writeTo(LocalVfs("/tmp/out.jpg"))
	}

	@Test
	fun png32() = suspendTest {
		val bitmap = root["kotlin32.png"].readBitmapNoNative(imageFormats)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun tga() = suspendTest {
		val bitmap = root["kotlin.tga"].readBitmapNoNative(imageFormats)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun jpeg() = suspendTest {
		val bitmap = root["kotlin.jpg"].readBitmapNoNative(imageFormats)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun jpeg2() = suspendTest {
		val bitmap = root["img1.jpg"].readBitmapNoNative(imageFormats)
		assertEquals("Bitmap32(460, 460)", bitmap.toString())
	}

	@Test
	fun ico() = suspendTest {
		val bitmaps = root["icon.ico"].readBitmapListNoNative(imageFormats)
		assertEquals(
			"[Bitmap32(256, 256), Bitmap32(128, 128), Bitmap32(96, 96), Bitmap32(72, 72), Bitmap32(64, 64), Bitmap32(48, 48), Bitmap32(32, 32), Bitmap32(24, 24), Bitmap32(16, 16)]",
			bitmaps.toString()
		)
	}

	//@Test
	////@Ignore
	//fun huge() = imageTest {
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