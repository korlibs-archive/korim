package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Test
import kotlin.test.assertEquals

class ImageFormatsTest {
	fun imageTest(callback: suspend () -> Unit) = syncTest {
		defaultImageFormats.temporalFormats(StandardImageFormats + SVG + ICO) {
			callback()
		}
	}

	//@kotlin.test.Test
	//fun demo1() = imageTest {
	//  val tempVfs = LocalVfs("c:/temp/")
	//	tempVfs["1.png"].readBitmap().writeTo(tempVfs["1.out.png"])
	//	Bitmap32(32, 32, premult = true) { x, y -> if ((x + y) % 2 == 0) Colors.RED else Colors.BLUE }.writeTo(tempVfs["red.png"])
	//	//println("ResourcesVfs.absolutePath:" + ResourcesVfs.absolutePath)
	//}


	@kotlin.test.Test
	fun png8() = imageTest {
		//println("ResourcesVfs.absolutePath:" + ResourcesVfs.absolutePath)
		val bitmap = ResourcesVfs["kotlin8.png"].readBitmapNoNative()
		assertEquals("Bitmap8(190, 190, palette=32)", bitmap.toString())
	}

	@kotlin.test.Test
	fun png24() = imageTest {
		val bitmap = ResourcesVfs["kotlin24.png"].readBitmapNoNative()
		//JailedLocalVfs("c:/temp/")["lol.png"].writeBitmap(bitmap, formats)
		//ResourcesVfs["kotlin8.png"].writeBitmap()
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@kotlin.test.Test
	fun png32Encoder() = imageTest {
		val bitmap = ResourcesVfs["kotlin24.png"].readBitmapNoNative()
		val data = PNG.encode(bitmap)
		val bitmap2 = PNG.decode(data)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
		assertEquals("Bitmap32(190, 190)", bitmap2.toString())
		assertEquals(true, Bitmap32.matches(bitmap, bitmap2))
	}

	@kotlin.test.Test
	fun png32EncoderPremultiplied() = imageTest {
		val bitmapOriginal = ResourcesVfs["kotlin32.png"].readBitmapNoNative().toBMP32()
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
	fun jpegEncoder() = imageTest {
		val bitmapOriginal = ResourcesVfs["kotlin32.png"].readBitmapNoNative().toBMP32()
		//val bitmapOriginal = LocalVfs("/tmp/kotlin32.png").readBitmapNoNative().toBMP32()
		val bytes = JPEG.encode(bitmapOriginal, ImageEncodingProps(quality = 0.5))
		//val bitmapOriginal = LocalVfs("/tmp/aa.jpg").readBitmapNoNative().toBMP32()
		//bitmapOriginal.writeTo(LocalVfs("/tmp/out.jpg"))
	}

	@kotlin.test.Test
	fun png32() = imageTest {
		val bitmap = ResourcesVfs["kotlin32.png"].readBitmapNoNative()
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@kotlin.test.Test
	fun tga() = imageTest {
		val bitmap = ResourcesVfs["kotlin.tga"].readBitmapNoNative()
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@kotlin.test.Test
	fun jpeg() = imageTest {
		val bitmap = ResourcesVfs["kotlin.jpg"].readBitmapNoNative()
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@kotlin.test.Test
	fun jpeg2() = imageTest {
		val bitmap = ResourcesVfs["img1.jpg"].readBitmapNoNative()
		assertEquals("Bitmap32(460, 460)", bitmap.toString())
	}

	@kotlin.test.Test
	fun ico() = imageTest {
		val bitmaps = ResourcesVfs["icon.ico"].readBitmapListNoNative()
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