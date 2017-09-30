package com.soywiz.korim.color

import org.junit.Test
import kotlin.test.assertEquals

class RGBAfTest {
	@Test
	fun name() {
		assertEquals("RGBAf(0.5, 4, 1, 7)", RGBAf(0.5, 4, 1, 7).toString())
	}
}