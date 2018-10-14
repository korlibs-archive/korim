package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class ImageFormatsNativeTest {
	val formats = ImageFormats(PNG, ICO, SVG)

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
	fun svg() = suspendTest {
		val bi = ResourcesVfs["logo.svg"].readBitmapInfo(formats)!!
		assertEquals(Size(60, 60), bi.size)
		val bitmap = ResourcesVfs["logo.svg"].readBitmap(formats = formats)
		//showImageAndWait(bitmap)
		//File("c:/temp/logosvg.png").toVfs().writeBitmap(bitmap.toBMP32())
	}
}
