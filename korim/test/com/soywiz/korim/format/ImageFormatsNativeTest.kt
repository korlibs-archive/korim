package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.async.sync
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.toVfs
import com.soywiz.korma.geom.Size
import org.junit.Assert
import org.junit.Test
import java.io.File

class ImageFormatsNativeTest {
	@Test
	fun png8() = syncTest {
		val bitmap = ResourcesVfs["kotlin8.png"].readNativeImage()
		Assert.assertEquals("AwtNativeImage(190, 190)", bitmap.toString())
		//awtShowImage(bitmap); Thread.sleep(10000L)
	}

	@Test
	fun png24() = syncTest {
		val bitmap = ResourcesVfs["kotlin24.png"].readBitmap()
		Assert.assertEquals("AwtNativeImage(190, 190)", bitmap.toString())
		//awtShowImage(bitmap); Thread.sleep(10000L)
	}


	@Test
	fun png32() = syncTest {
		val bitmap = ResourcesVfs["kotlin32.png"].readBitmap()
		Assert.assertEquals("AwtNativeImage(190, 190)", bitmap.toString())
		//awtShowImage(bitmap); Thread.sleep(10000L)
	}

	@Test
	fun jpeg() = syncTest {
		val bitmap = ResourcesVfs["kotlin.jpg"].readBitmap()
		Assert.assertEquals("AwtNativeImage(190, 190)", bitmap.toString())

		val bitmapExpected = ResourcesVfs["kotlin.jpg.png"].readBitmap()
		Assert.assertTrue(Bitmap32.matches(bitmapExpected, bitmap))

		//val diff = Bitmap32.diff(bitmapExpected, bitmap)
		//diff.transformColor { RGBA.pack(RGBA.getR(it) * 0xFF, RGBA.getG(it) * 0xFF, RGBA.getB(it) * 0xFF, 0xFF) }
		//awtShowImage(diff); Thread.sleep(10000L)
	}

	@Test
	fun jpeg2() = syncTest {
		val bitmap = ResourcesVfs["img1.jpg"].readBitmap()
		Assert.assertEquals("AwtNativeImage(460, 460)", bitmap.toString())

		val bitmapExpected = ResourcesVfs["img1.jpg.png"].readBitmap()
		Assert.assertTrue(Bitmap32.matches(bitmapExpected, bitmap, threshold = 32))

		//val diff = Bitmap32.diff(bitmapExpected, bitmap)
		//diff.transformColor { RGBA.pack(RGBA.getR(it) * 4, RGBA.getG(it) * 4, RGBA.getB(it) * 4, 0xFF) }
		//diff.transformColor { RGBA.pack(RGBA.getR(it) * 0xFF, RGBA.getG(it) * 0xFF, RGBA.getB(it) * 0xFF, 0xFF) }
		//awtShowImage(diff); Thread.sleep(10000L)
	}

	@Test
	fun svg() = syncTest {
		val bi = ResourcesVfs["logo.svg"].readBitmapInfo()!!
		Assert.assertEquals(Size(128, 128), bi.size)
		val bitmap = ResourcesVfs["logo.svg"].readBitmap()
		//showImageAndWait(bitmap)
		//File("c:/temp/logosvg.png").toVfs().writeBitmap(bitmap.toBMP32())
	}
}