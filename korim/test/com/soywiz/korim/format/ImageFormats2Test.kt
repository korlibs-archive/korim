package com.soywiz.korim.format

import com.soywiz.korim.awt.awtShowImage
import com.soywiz.korim.bitmap.mipmap
import com.soywiz.korio.async.sync
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class ImageFormats2Test {
	@Test
	fun png8() = syncTest {
		val bitmap = ResourcesVfs["kotlin8.png"].readBitmapNoNative()
		Assert.assertEquals("Bitmap8(190, 190, palette=32)", bitmap.toString())
		//awtShowImage(bitmap); Thread.sleep(10000L)
	}

	@Test
	fun png24() = syncTest {
		val bitmap = ResourcesVfs["kotlin24.png"].readBitmapNoNative()
		Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
		//val bitmap2 = bitmap.toBMP32().mipmap(2)
		//val bitmap2 = bitmap.toBMP32()
		//awtShowImage(bitmap2); Thread.sleep(10000L)
	}

	@Test
	@Ignore
	fun mipmaps() = syncTest {
		val bitmap = ResourcesVfs["kotlin24.png"].readBitmapNoNative()
		Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
		val bitmap2 = bitmap.toBMP32().mipmap(2)
		awtShowImage(bitmap2); Thread.sleep(10000L)
	}

	@Test
	fun png32() = syncTest {
		val bitmap = ResourcesVfs["kotlin32.png"].readBitmapNoNative()
		Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
		//awtShowImage(bitmap); Thread.sleep(10000L)
	}

	@Test
	fun jpeg() = syncTest {
		val bitmap = ResourcesVfs["kotlin.jpg"].readBitmapNoNative()
		Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun jpeg2() = syncTest {
		val bitmap = ResourcesVfs["img1.jpg"].readBitmapNoNative()
		Assert.assertEquals("Bitmap32(460, 460)", bitmap.toString())
	}

	@Test
	fun ico() = syncTest {
		val bitmaps = ResourcesVfs["icon.ico"].readBitmapListNoNative()
		Assert.assertEquals(
				"[Bitmap32(256, 256), Bitmap32(128, 128), Bitmap32(96, 96), Bitmap32(72, 72), Bitmap32(64, 64), Bitmap32(48, 48), Bitmap32(32, 32), Bitmap32(24, 24), Bitmap32(16, 16)]",
				bitmaps.toString()
		)
	}
}