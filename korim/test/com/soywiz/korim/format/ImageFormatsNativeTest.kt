package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.async.sync
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Assert
import org.junit.Test

class ImageFormatsNativeTest {
	@Test
	fun png() = sync {
		val bitmap = ResourcesVfs["kotlin.png"].readBitmap()
		Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun png8() = sync {
		val bitmap = ResourcesVfs["kotlin8.png"].readBitmap()
		Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun jpeg() = sync {
		val bitmap = ResourcesVfs["kotlin.jpg"].readBitmap()
		Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())

		val bitmapExpected = ResourcesVfs["kotlin.jpg.png"].readBitmap()
		Assert.assertTrue(Bitmap32.matches(bitmapExpected, bitmap))

		//val diff = Bitmap32.diff(bitmapExpected, bitmap)
		//diff.transformColor { RGBA.pack(RGBA.getR(it) * 0xFF, RGBA.getG(it) * 0xFF, RGBA.getB(it) * 0xFF, 0xFF) }
		//awtShowImage(diff); Thread.sleep(10000L)
	}

	@Test
	fun jpeg2() = sync {
		val bitmap = ResourcesVfs["img1.jpg"].readBitmap()
		Assert.assertEquals("Bitmap32(460, 460)", bitmap.toString())

		val bitmapExpected = ResourcesVfs["img1.jpg.png"].readBitmap()
		Assert.assertTrue(Bitmap32.matches(bitmapExpected, bitmap, threshold = 32))

		//val diff = Bitmap32.diff(bitmapExpected, bitmap)
		//diff.transformColor { RGBA.pack(RGBA.getR(it) * 4, RGBA.getG(it) * 4, RGBA.getB(it) * 4, 0xFF) }
		//diff.transformColor { RGBA.pack(RGBA.getR(it) * 0xFF, RGBA.getG(it) * 0xFF, RGBA.getB(it) * 0xFF, 0xFF) }
		//awtShowImage(diff); Thread.sleep(10000L)
	}
}