package com.soywiz.korim.color

import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class RGBAfTest {
	@Test
	fun name() {
		Assert.assertEquals("RGBAf(0.5, 4, 1, 7)", RGBAf(0.5, 4, 1, 7).toString())
	}
}