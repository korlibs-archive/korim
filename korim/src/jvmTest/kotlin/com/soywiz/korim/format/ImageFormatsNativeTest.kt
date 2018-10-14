package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class ImageFormatsNativeTest {
	val formats = ImageFormats(StandardImageFormats + ICO + SVG)

	@Test
	fun png8() = suspendTest {
		val bitmap = ResourcesVfs["kotlin8.png"].readNativeImage()
		assertEquals("AwtNativeImage(190, 190)", bitmap.toString())
		//awtShowImage(bitmap); Thread.sleep(10000L)
	}

	@Test
	fun png24() = suspendTest {
		val bitmap = ResourcesVfs["kotlin24.png"].readBitmap(formats = formats)
		assertEquals("AwtNativeImage(190, 190)", bitmap.toString())
		//awtShowImage(bitmap); Thread.sleep(10000L)
	}


	@Test
	fun png32() = suspendTest {
		val bitmap = ResourcesVfs["kotlin32.png"].readBitmap(formats = formats)
		assertEquals("AwtNativeImage(190, 190)", bitmap.toString())
		//awtShowImage(bitmap); Thread.sleep(10000L)
	}

	@Test
	fun jpeg() = suspendTest {
		val bitmap = ResourcesVfs["kotlin.jpg"].readBitmap(formats = formats)
		assertEquals("AwtNativeImage(190, 190)", bitmap.toString())

		val bitmapExpected = ResourcesVfs["kotlin.jpg.png"].readBitmap(formats = formats)
		assertTrue(Bitmap32.matches(bitmapExpected, bitmap))

		//val diff = Bitmap32.diff(bitmapExpected, bitmap)
		//diff.transformColor { RGBA.pack(RGBA.getR(it) * 0xFF, RGBA.getG(it) * 0xFF, RGBA.getB(it) * 0xFF, 0xFF) }
		//awtShowImage(diff); Thread.sleep(10000L)
	}

	@Test
	fun jpeg2() = suspendTest {
		val bitmap = ResourcesVfs["img1.jpg"].readBitmap(formats = formats)
		assertEquals("AwtNativeImage(460, 460)", bitmap.toString())

		val bitmapExpected = ResourcesVfs["img1.jpg.png"].readBitmap(formats = formats)
		assertTrue(Bitmap32.matches(bitmapExpected, bitmap, threshold = 32))

		//val diff = Bitmap32.diff(bitmapExpected, bitmap)
		//diff.transformColor { RGBA.pack(RGBA.getR(it) * 4, RGBA.getG(it) * 4, RGBA.getB(it) * 4, 0xFF) }
		//diff.transformColor { RGBA.pack(RGBA.getR(it) * 0xFF, RGBA.getG(it) * 0xFF, RGBA.getB(it) * 0xFF, 0xFF) }
		//awtShowImage(diff); Thread.sleep(10000L)
	}

	@Test
	fun svg() = suspendTest {
		val bi = ResourcesVfs["logo.svg"].readBitmapInfo(formats)!!
		assertEquals(Size(60, 60), bi.size)
		val bitmap = ResourcesVfs["logo.svg"].readBitmap(formats = formats)
		//showImageAndWait(bitmap)
		//File("c:/temp/logosvg.png").toVfs().writeBitmap(bitmap.toBMP32())
	}
}