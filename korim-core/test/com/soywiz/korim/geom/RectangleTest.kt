package com.soywiz.korim.geom

import org.junit.Assert
import org.junit.Test

class RectangleTest {
	@Test
	fun name() {
		val big = Rectangle.fromBounds(0, 0, 50, 50)
		val small = Rectangle.fromBounds(10, 10, 20, 20)
		val out = Rectangle.fromBounds(100, 10, 200, 20)
		Assert.assertTrue(small in big)
		Assert.assertTrue(big !in small)
		Assert.assertTrue(small == (small intersection big))
		Assert.assertTrue(small == (big intersection small))
		Assert.assertTrue(null == (big intersection out))
		Assert.assertTrue(small intersects big)
		Assert.assertTrue(big intersects small)
		Assert.assertFalse(big intersects out)
	}
}