package com.soywiz.korim.geom

import org.junit.Assert
import org.junit.Test

class ISizeTest {
	@Test
	fun cover() {
		Assert.assertEquals(ISize(100, 400), ISize(50, 200).applyScaleMode(container = ISize(100, 100), mode = ScaleMode.COVER))
		Assert.assertEquals(ISize(25, 100), ISize(50, 200).applyScaleMode(container = ISize(25, 25), mode = ScaleMode.COVER))
	}
}