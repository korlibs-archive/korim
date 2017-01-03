package com.soywiz.korim.geom

import org.junit.Assert
import org.junit.Test

class IRectangleTest {
	@Test
	fun name() {
		Assert.assertEquals(ISize(25, 100), ISize(50, 200).fitTo(container = ISize(100, 100)))
		Assert.assertEquals(ISize(50, 200), ISize(50, 200).fitTo(container = ISize(100, 200)))
	}
}