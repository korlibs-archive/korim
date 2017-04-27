package com.soywiz.korim.color

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class RGBATest {
	@Test
	fun premultiply() {
		Assert.assertEquals("#7f7f7f7f", RGBA.toHexString(RGBA.premultiply(RGBA.packFast(0xFF, 0xFF, 0xFF, 0x7F))))
		Assert.assertEquals("#7f7f7f7f", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0xFF, 0xFF, 0xFF, 0x7F))))
		Assert.assertEquals("#ffffffff", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0xFF, 0xFF, 0xFF, 0xFF))))
		Assert.assertEquals("#0000007f", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0x00, 0x00, 0x00, 0x7F))))
		Assert.assertEquals("#3f3f3f7f", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0x7F, 0x7F, 0x7F, 0x7F))))
		Assert.assertEquals("#001f3f7f", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0x00, 0x3F, 0x7F, 0x7F))))
	}

	@Test
	fun depremultiply() {
		Assert.assertEquals("#007fffff", RGBA.toHexString(RGBA.depremultiplyAccurate(Colors["#007fffff"])))
		Assert.assertEquals("#007fffff", RGBA.toHexString(RGBA.depremultiplyFast(Colors["#007fffff"])))
		Assert.assertEquals("#007fffff", RGBA.toHexString(RGBA.depremultiplyFaster(Colors["#007fffff"])))
		Assert.assertEquals("#007fffff", RGBA.toHexString(RGBA.depremultiplyFastest(Colors["#007fffff"])))

		Assert.assertEquals("#2666ff7f", RGBA.toHexString(RGBA.depremultiplyAccurate(Colors["#13337f7f"])))
		Assert.assertEquals("#2666ff7f", RGBA.toHexString(RGBA.depremultiplyFast(Colors["#13337f7f"])))
		Assert.assertEquals("#2666fe7f", RGBA.toHexString(RGBA.depremultiplyFaster(Colors["#13337f7f"])))
		Assert.assertEquals("#2066fe7f", RGBA.toHexString(RGBA.depremultiplyFastest(Colors["#13337f7f"])))

		Assert.assertEquals("#00ffff7f", RGBA.toHexString(RGBA.depremultiplyAccurate(Colors["#007fff7f"])))
		Assert.assertEquals("#00ffff7f", RGBA.toHexString(RGBA.depremultiplyFast(Colors["#007fff7f"])))
		Assert.assertEquals("#00fefe7f", RGBA.toHexString(RGBA.depremultiplyFaster(Colors["#007fff7f"])))
		Assert.assertEquals("#00fefe7f", RGBA.toHexString(RGBA.depremultiplyFastest(Colors["#007fff7f"])))

		Assert.assertEquals("#00ffff3f", RGBA.toHexString(RGBA.depremultiplyAccurate(Colors["#007fff3f"])))
		Assert.assertEquals("#00ffff3f", RGBA.toHexString(RGBA.depremultiplyFast(Colors["#007fff3f"])))
		Assert.assertEquals("#00fcfc3f", RGBA.toHexString(RGBA.depremultiplyFaster(Colors["#007fff3f"])))
		Assert.assertEquals("#00fcfc3f", RGBA.toHexString(RGBA.depremultiplyFastest(Colors["#007fff3f"])))

		Assert.assertEquals("#00000000", RGBA.toHexString(RGBA.depremultiplyAccurate(Colors["#007fff00"])))
		Assert.assertEquals("#00000000", RGBA.toHexString(RGBA.depremultiplyFast(Colors["#007fff00"])))
		Assert.assertEquals("#00000000", RGBA.toHexString(RGBA.depremultiplyFaster(Colors["#007fff00"])))
		Assert.assertEquals("#00000000", RGBA.toHexString(RGBA.depremultiplyFastest(Colors["#007fff00"])))
	}

	@Test
	fun name2() {
		Assert.assertEquals("#123456ff", RGBA.toHexString(Colors["#123456"]))
		Assert.assertEquals("#12345678", RGBA.toHexString(Colors["#12345678"]))

		Assert.assertEquals("#000000ff", RGBA.toHexString(Colors["#000"]))
		Assert.assertEquals("#777777ff", RGBA.toHexString(Colors["#777"]))
		Assert.assertEquals("#ffffffff", RGBA.toHexString(Colors["#FFF"]))

		Assert.assertEquals("#00000000", RGBA.toHexString(Colors["#0000"]))
		Assert.assertEquals("#77777700", RGBA.toHexString(Colors["#7770"]))
		Assert.assertEquals("#ffffff00", RGBA.toHexString(Colors["#FFF0"]))
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

	@Test
	@Ignore
	fun benchmarkDepremultiplyAccurate() {
		var m = 0
		for (n in 0 until 8192 * 8192) m += RGBA.depremultiplyAccurate(colors[n and 1])
	}

	@Test
	@Ignore
	fun benchmarkDepremultiplyFast() {
		var m = 0
		for (n in 0 until 8192 * 8192) m += RGBA.depremultiplyFast(colors[n and 1])
	}

	@Test
	@Ignore
	fun benchmarkDepremultiplyFastest() {
		var m = 0
		for (n in 0 until 8192 * 8192) m += RGBA.depremultiplyFastest(colors[n and 1])
	}

	//@Test
	//@Ignore
	//fun benchmark3() {
	//	var m = 0
	//	val c = RGBA.pack(0xFF, 0xFF, 0xFF, 0x7F)
	//	for (n in 0 until 10000000000) m += RGBA.premultiplyFast2(c)
	//}
}