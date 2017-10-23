package com.soywiz.korim.color

import kotlin.test.assertEquals

class RGBATest {
	@kotlin.test.Test
	fun premultiply() {
		assertEquals("#7f7f7f7f", RGBA.toHexString(RGBA.premultiply(RGBA.packFast(0xFF, 0xFF, 0xFF, 0x7F))))
		assertEquals("#7f7f7f7f", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0xFF, 0xFF, 0xFF, 0x7F))))
		assertEquals("#ffffffff", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0xFF, 0xFF, 0xFF, 0xFF))))
		assertEquals("#0000007f", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0x00, 0x00, 0x00, 0x7F))))
		assertEquals("#3f3f3f7f", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0x7F, 0x7F, 0x7F, 0x7F))))
		assertEquals("#001f3f7f", RGBA.toHexString(RGBA.premultiplyFast(RGBA.packFast(0x00, 0x3F, 0x7F, 0x7F))))
	}

	@kotlin.test.Test
	fun depremultiply() {
		assertEquals("#007fffff", RGBA.toHexString(RGBA.depremultiplyAccurate(Colors["#007fffff"])))
		assertEquals("#007fffff", RGBA.toHexString(RGBA.depremultiplyFast(Colors["#007fffff"])))
		assertEquals("#007fffff", RGBA.toHexString(RGBA.depremultiplyFaster(Colors["#007fffff"])))
		assertEquals("#007fffff", RGBA.toHexString(RGBA.depremultiplyFastest(Colors["#007fffff"])))

		assertEquals("#2666ff7f", RGBA.toHexString(RGBA.depremultiplyAccurate(Colors["#13337f7f"])))
		assertEquals("#2666ff7f", RGBA.toHexString(RGBA.depremultiplyFast(Colors["#13337f7f"])))
		assertEquals("#2666fe7f", RGBA.toHexString(RGBA.depremultiplyFaster(Colors["#13337f7f"])))
		assertEquals("#2066fe7f", RGBA.toHexString(RGBA.depremultiplyFastest(Colors["#13337f7f"])))

		assertEquals("#00ffff7f", RGBA.toHexString(RGBA.depremultiplyAccurate(Colors["#007fff7f"])))
		assertEquals("#00ffff7f", RGBA.toHexString(RGBA.depremultiplyFast(Colors["#007fff7f"])))
		assertEquals("#00fefe7f", RGBA.toHexString(RGBA.depremultiplyFaster(Colors["#007fff7f"])))
		assertEquals("#00fefe7f", RGBA.toHexString(RGBA.depremultiplyFastest(Colors["#007fff7f"])))

		assertEquals("#00ffff3f", RGBA.toHexString(RGBA.depremultiplyAccurate(Colors["#007fff3f"])))
		assertEquals("#00ffff3f", RGBA.toHexString(RGBA.depremultiplyFast(Colors["#007fff3f"])))
		assertEquals("#00fcfc3f", RGBA.toHexString(RGBA.depremultiplyFaster(Colors["#007fff3f"])))
		assertEquals("#00fcfc3f", RGBA.toHexString(RGBA.depremultiplyFastest(Colors["#007fff3f"])))

		assertEquals("#00000000", RGBA.toHexString(RGBA.depremultiplyAccurate(Colors["#007fff00"])))
		assertEquals("#00000000", RGBA.toHexString(RGBA.depremultiplyFast(Colors["#007fff00"])))
		assertEquals("#00000000", RGBA.toHexString(RGBA.depremultiplyFaster(Colors["#007fff00"])))
		assertEquals("#00000000", RGBA.toHexString(RGBA.depremultiplyFastest(Colors["#007fff00"])))
	}

	@kotlin.test.Test
	fun name2() {
		assertEquals("#123456ff", RGBA.toHexString(Colors["#123456"]))
		assertEquals("#12345678", RGBA.toHexString(Colors["#12345678"]))

		assertEquals("#000000ff", RGBA.toHexString(Colors["#000"]))
		assertEquals("#777777ff", RGBA.toHexString(Colors["#777"]))
		assertEquals("#ffffffff", RGBA.toHexString(Colors["#FFF"]))

		assertEquals("#00000000", RGBA.toHexString(Colors["#0000"]))
		assertEquals("#77777700", RGBA.toHexString(Colors["#7770"]))
		assertEquals("#ffffff00", RGBA.toHexString(Colors["#FFF0"]))
	}

	val colors = intArrayOf(RGBA.pack(0xFF, 0xFF, 0xFF, 0x7F), RGBA.pack(0x7F, 0x6F, 0x33, 0x90))

	//@Test
	////@Ignore
	//fun benchmarkPremultiplyAccurate() {
	//	var m = 0
	//	for (n in 0 until 8192 * 8192) m += RGBA.premultiplyAccurate(colors[n and 1])
	//}

	//@Test
	////@Ignore
	//fun benchmarkPremultiplyFast() {
	//	var m = 0
	//	for (n in 0 until 8192 * 8192) m += RGBA.premultiplyFast(colors[n and 1])
	//}

	//@Test
	////@Ignore
	//fun benchmarkDepremultiplyAccurate() {
	//	var m = 0
	//	for (n in 0 until 8192 * 8192) m += RGBA.depremultiplyAccurate(colors[n and 1])
	//}

	//@Test
	////@Ignore
	//fun benchmarkDepremultiplyFast() {
	//	var m = 0
	//	for (n in 0 until 8192 * 8192) m += RGBA.depremultiplyFast(colors[n and 1])
	//}

	//@Test
	////@Ignore
	//fun benchmarkDepremultiplyFastest() {
	//	var m = 0
	//	for (n in 0 until 8192 * 8192) m += RGBA.depremultiplyFastest(colors[n and 1])
	//}

	//@Test
	////@Ignore
	//fun benchmark3() {
	//	var m = 0
	//	val c = RGBA.pack(0xFF, 0xFF, 0xFF, 0x7F)
	//	for (n in 0 until 10000000000) m += RGBA.premultiplyFast2(c)
	//}
}