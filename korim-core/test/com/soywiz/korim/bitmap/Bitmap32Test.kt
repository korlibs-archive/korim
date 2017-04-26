package com.soywiz.korim.bitmap

import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.readBitmapNoNative
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class Bitmap32Test {
	@Test
	fun name() {
		val c = Bitmap32(1, 1)
		c[0, 0] = Colors.WHITE
		Assert.assertEquals(Colors.WHITE, c[0, 0])
	}

	@Test
	fun constructGen() {
		val c = Bitmap32(1, 1) { _, _ -> Colors.BLUE }
		Assert.assertTrue(c.all { it == Colors.BLUE })
	}

	@Test
	fun fill() {
		val c = Bitmap32(16, 16)
		c.fill(Colors.RED)
		Assert.assertTrue(c.all { it == Colors.RED })
	}

	@Test
	@Ignore
	fun mipmaps() = syncTest {
		val bmp = Bitmap32(4096 * 2, 4096 * 2, Colors.BLACK)
		val bitmap2 = bmp.mipmap(2)
		//awtShowImage(bitmap2); Thread.sleep(10000L)
	}
}