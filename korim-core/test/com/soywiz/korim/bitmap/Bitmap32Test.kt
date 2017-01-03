package com.soywiz.korim.bitmap

import com.soywiz.korim.color.Colors
import org.junit.Assert
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
}