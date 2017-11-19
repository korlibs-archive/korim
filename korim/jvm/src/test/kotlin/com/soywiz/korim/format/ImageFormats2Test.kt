package com.soywiz.korim.format

import com.soywiz.korim.awt.awtShowImage
import com.soywiz.korim.bitmap.matchContents
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korio.vfs.applicationVfs
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageFormats2Test {
	val formats = ImageFormats().registerStandard().register(ICO)
	//val root = ResourcesVfs
	lateinit var root: VfsFile

	@Before
	fun before() = syncTest {
		for (path in listOf(applicationVfs["src/test/resources"], ResourcesVfs)) {
			root = path
			if (root["kotlin8.png"].exists()) break
		}
	}

	@Test
	fun png8() = syncTest {
		val bitmap = root["kotlin8.png"].readBitmapNoNative(formats)
		assertEquals("Bitmap8(190, 190, palette=32)", bitmap.toString())
		//awtShowImage(bitmap); Thread.sleep(10000L)
	}

	@Test
	fun png24() = syncTest {
		val bitmap = root["kotlin24.png"].readBitmapNoNative(formats)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
		//val bitmap2 = bitmap.toBMP32().mipmap(2)
		//val bitmap2 = bitmap.toBMP32()
		//awtShowImage(bitmap2); Thread.sleep(10000L)
	}

	@Test
	@Ignore
	fun mipmaps() = syncTest {
		val bitmap = root["kotlin24.png"].readBitmapNoNative(formats)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
		val bitmap2 = bitmap.toBMP32().mipmap(2)
		awtShowImage(bitmap2); Thread.sleep(10000L)
	}

	@Test
	fun png32() = syncTest {
		val bitmap = root["kotlin32.png"].readBitmapNoNative(formats)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
		//awtShowImage(bitmap); Thread.sleep(10000L)
		//bitmap.writeTo(LocalVfs("c:/temp/img1.jpg.png"), formats = formats)
	}

	@Test
	fun jpeg() = syncTest {
		val bitmap = root["kotlin.jpg"].readBitmapNoNative(formats)
		assertEquals("Bitmap32(190, 190)", bitmap.toString())
		//bitmap.writeTo(LocalVfs("c:/temp/img1.jpg.png"), formats = formats)
	}

	@Test
	fun jpeg2() = syncTest {
		val bitmap = root["img1.jpg"].readBitmapNoNative(formats)
		assertEquals("Bitmap32(460, 460)", bitmap.toString())
		//bitmap.writeTo(LocalVfs("c:/temp/img1.jpg.tga"), formats = formats)
	}

	@Test
	fun ico() = syncTest {
		val bitmaps = root["icon.ico"].readBitmapListNoNative(formats)
		assertEquals(
			"[Bitmap32(256, 256), Bitmap32(128, 128), Bitmap32(96, 96), Bitmap32(72, 72), Bitmap32(64, 64), Bitmap32(48, 48), Bitmap32(32, 32), Bitmap32(24, 24), Bitmap32(16, 16)]",
			bitmaps.toString()
		)
	}

	@Test
	fun pngInterlaced() = syncTest {
		val bitmap1 = root["icon0.png"].readBitmapNoNative(formats)
		val bitmap2 = root["icon0.deinterlaced.png"].readBitmapNoNative(formats)
		assertTrue(bitmap1.matchContents(bitmap2))
		//bitmap1.writeTo(LocalVfs("c:/temp/demo1.png"), formats = formats)
		//bitmap2.writeTo(LocalVfs("c:/temp/demo2.png"), formats = formats)
	}
}