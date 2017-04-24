package com.soywiz.korim.color

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class RGBATest {
	@Test
	fun name() {
		Assert.assertEquals("#7f7f7f7f", RGBA.toHexString(RGBA.premultiply(RGBA.packFast(0xFF, 0xFF, 0xFF, 0x7F))))
		Assert.assertEquals("#7f7f7f7f", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0xFF, 0xFF, 0xFF, 0x7F))))
		Assert.assertEquals("#ffffffff", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0xFF, 0xFF, 0xFF, 0xFF))))
		Assert.assertEquals("#0000007f", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0x00, 0x00, 0x00, 0x7F))))
		Assert.assertEquals("#3f3f3f7f", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0x7F, 0x7F, 0x7F, 0x7F))))
		Assert.assertEquals("#001f3f7f", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0x00, 0x3F, 0x7F, 0x7F))))
	}

	val colors = intArrayOf(RGBA.pack(0xFF, 0xFF, 0xFF, 0x7F), RGBA.pack(0x7F, 0x6F, 0x33, 0x90))

	@Test
	@Ignore
	fun benchmarkPremultiplyAccurate() {
		var m = 0
		for (n in 0 until 8192 * 8192) m += RGBA.premultiplyAccurate(colors[n and 1])
	}

	@Test
	@Ignore
	fun benchmarkPremultiplyFast() {
		var m = 0
		for (n in 0 until 8192 * 8192) m += RGBA.premultiplyFast(colors[n and 1])
	}

	//@Test
	//@Ignore
	//fun benchmark3() {
	//	var m = 0
	//	val c = RGBA.pack(0xFF, 0xFF, 0xFF, 0x7F)
	//	for (n in 0 until 10000000000) m += RGBA.premultiplyFast2(c)
	//}
}