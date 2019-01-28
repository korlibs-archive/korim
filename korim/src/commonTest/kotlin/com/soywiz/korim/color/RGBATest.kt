package com.soywiz.korim.color

import kotlin.test.*

class RGBATest {
	@Test
	fun premultiply() {
		assertEquals("#7f7f7f7f", RGBA.premultiply(RGBA(0xFF, 0xFF, 0xFF, 0x7F)).hexString)
		assertEquals("#7f7f7f7f", RGBA.premultiplyFast(RGBA(0xFF, 0xFF, 0xFF, 0x7F)).hexString)
		assertEquals("#ffffffff", RGBA.premultiplyFast(RGBA(0xFF, 0xFF, 0xFF, 0xFF)).hexString)
		assertEquals("#0000007f", RGBA.premultiplyFast(RGBA(0x00, 0x00, 0x00, 0x7F)).hexString)
		assertEquals("#3f3f3f7f", RGBA.premultiplyFast(RGBA(0x7F, 0x7F, 0x7F, 0x7F)).hexString)
		assertEquals("#001f3f7f", RGBA.premultiplyFast(RGBA(0x00, 0x3F, 0x7F, 0x7F)).hexString)
	}

	@Test
	fun depremultiply() {
		assertEquals("#007fffff", RGBA.depremultiplyAccurate(Colors["#007fffff"]).hexString)
		assertEquals("#007fffff", RGBA.depremultiplyFast(Colors["#007fffff"]).hexString)
		assertEquals("#007fffff", RGBA.depremultiplyFaster(Colors["#007fffff"]).hexString)
		assertEquals("#007fffff", RGBA.depremultiplyFastest(Colors["#007fffff"]).hexString)

		assertEquals("#2666ff7f", RGBA.depremultiplyAccurate(Colors["#13337f7f"]).hexString)
		assertEquals("#2666ff7f", RGBA.depremultiplyFast(Colors["#13337f7f"]).hexString)
		assertEquals("#2666fe7f", RGBA.depremultiplyFaster(Colors["#13337f7f"]).hexString)
		assertEquals("#2066fe7f", RGBA.depremultiplyFastest(Colors["#13337f7f"]).hexString)

		assertEquals("#00ffff7f", RGBA.depremultiplyAccurate(Colors["#007fff7f"]).hexString)
		assertEquals("#00ffff7f", RGBA.depremultiplyFast(Colors["#007fff7f"]).hexString)
		assertEquals("#00fefe7f", RGBA.depremultiplyFaster(Colors["#007fff7f"]).hexString)
		assertEquals("#00fefe7f", RGBA.depremultiplyFastest(Colors["#007fff7f"]).hexString)

		assertEquals("#00ffff3f", RGBA.depremultiplyAccurate(Colors["#007fff3f"]).hexString)
		assertEquals("#00ffff3f", RGBA.depremultiplyFast(Colors["#007fff3f"]).hexString)
		assertEquals("#00fcfc3f", RGBA.depremultiplyFaster(Colors["#007fff3f"]).hexString)
		assertEquals("#00fcfc3f", RGBA.depremultiplyFastest(Colors["#007fff3f"]).hexString)

		assertEquals("#00000000", RGBA.depremultiplyAccurate(Colors["#007fff00"]).hexString)
		assertEquals("#00000000", RGBA.depremultiplyFast(Colors["#007fff00"]).hexString)
		assertEquals("#00000000", RGBA.depremultiplyFaster(Colors["#007fff00"]).hexString)
		assertEquals("#00000000", RGBA.depremultiplyFastest(Colors["#007fff00"]).hexString)
	}

	@Test
	fun name2() {
		assertEquals("#123456ff", Colors["#123456"].hexString)
		assertEquals("#12345678", Colors["#12345678"].hexString)

		assertEquals("#000000ff", Colors["#000"].hexString)
		assertEquals("#777777ff", Colors["#777"].hexString)
		assertEquals("#ffffffff", Colors["#FFF"].hexString)

		assertEquals("#00000000", Colors["#0000"].hexString)
		assertEquals("#77777700", Colors["#7770"].hexString)
		assertEquals("#ffffff00", Colors["#FFF0"].hexString)
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
