package com.soywiz.korim.color

import kotlin.test.*

class RGBATest {
	@Test
	fun premultiply() {
		assertEquals("#7f7f7f7f", RGBA(0xFF, 0xFF, 0xFF, 0x7F).premultiplied.hexString)
		assertEquals("#7f7f7f7f", RGBA(0xFF, 0xFF, 0xFF, 0x7F).premultiplied.hexString)
		assertEquals("#ffffffff", RGBA(0xFF, 0xFF, 0xFF, 0xFF).premultiplied.hexString)
		assertEquals("#0000007f", RGBA(0x00, 0x00, 0x00, 0x7F).premultiplied.hexString)
		assertEquals("#3f3f3f7f", RGBA(0x7F, 0x7F, 0x7F, 0x7F).premultiplied.hexString)
		assertEquals("#001f3f7f", RGBA(0x00, 0x3F, 0x7F, 0x7F).premultiplied.hexString)
	}

	@Test
	fun depremultiply() {
		assertEquals("#007fffff", Colors["#007fffff"].asPremultiplied().depremultipliedAccurate.hexString)
		assertEquals("#007fffff", Colors["#007fffff"].asPremultiplied().depremultiplied.hexString)

		assertEquals("#2666ff7f", Colors["#13337f7f"].asPremultiplied().depremultipliedAccurate.hexString)
		assertEquals("#2666fe7f", Colors["#13337f7f"].asPremultiplied().depremultipliedFast.hexString)

		assertEquals("#00ffff7f", Colors["#007fff7f"].asPremultiplied().depremultipliedAccurate.hexString)
		assertEquals("#00fefe7f", Colors["#007fff7f"].asPremultiplied().depremultipliedFast.hexString)

		assertEquals("#00ffff3f", Colors["#007fff3f"].asPremultiplied().depremultipliedAccurate.hexString)
		assertEquals("#00fcfc3f", Colors["#007fff3f"].asPremultiplied().depremultipliedFast.hexString)

		assertEquals("#00000000", Colors["#007fff00"].asPremultiplied().depremultipliedAccurate.hexString)
		assertEquals("#00000000", Colors["#007fff00"].asPremultiplied().depremultipliedFast.hexString)
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

    @Test
    fun with() {
        assertEquals(Colors.RED, Colors.BLACK.withR(0xFF))
        assertEquals(Colors.FUCHSIA, Colors.RED.withB(0xFF))
        assertEquals(Colors.YELLOW, Colors.RED.withG(0xFF))
        assertEquals(Colors.TRANSPARENT_BLACK, Colors.BLACK.withA(0))
        assertEquals(Colors.TRANSPARENT_WHITE, Colors.WHITE.withA(0))
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

    @Test
    fun mix() {
        val dst = Colors["#00ff00ff"]
        val src = Colors["#3f3f3f7f"]
        //val out = dst mix src
        assertEquals("#1f9f1fff", (dst mix src).toString())
        assertEquals("#1f9f1fff", (dst.premultiplied mix src.premultiplied).depremultiplied.toString())
    }
}
