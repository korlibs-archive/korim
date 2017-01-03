package com.soywiz.korim.color

object RGBA : ColorFormat() {
	//private inline val R_SHIFT: Int get() = 0
	//private inline val G_SHIFT: Int get() = 8
	//private inline val B_SHIFT: Int get() = 16
	//private inline val A_SHIFT: Int get() = 24

	override fun getR(v: Int): Int = (v ushr 0) and 0xFF
	override fun getG(v: Int): Int = (v ushr 8) and 0xFF
	override fun getB(v: Int): Int = (v ushr 16) and 0xFF
	override fun getA(v: Int): Int = (v ushr 24) and 0xFF

	//fun getRGB(v: Int): Int = v and 0xFFFFFF

	@JvmStatic fun getRGB(v: Int): Int = v and 0xFFFFFF

	@JvmStatic fun packFast(r: Int, g: Int, b: Int, a: Int) = (r shl 0) or (g shl 8) or (b shl 16) or (a shl 24)
	@JvmStatic fun packfFast(r: Float, g: Float, b: Float, a: Float): Int = ((r * 255).toInt() shl 0) or ((g * 255).toInt() shl 8) or ((b * 255).toInt() shl 16) or ((a * 255).toInt() shl 24)

	override fun pack(r: Int, g: Int, b: Int, a: Int) = ((ColorFormat.clamp0_FF(r)) shl 0) or ((ColorFormat.clamp0_FF(g)) shl 8) or ((ColorFormat.clamp0_FF(b)) shl 16) or ((ColorFormat.clamp0_FF(a)) shl 24)

	@JvmStatic fun packRGB_A(rgb: Int, a: Int): Int = (rgb and 0xFFFFFF) or (a shl 24)

	@JvmStatic fun blend(c1: Int, c2: Int, factor: Int): Int {
		val f1 = 256 - factor
		return ((
			((((c1 and 0xFF00FF) * f1) + ((c2 and 0xFF00FF) * factor)) and 0xFF00FF00.toInt())
				or
				((((c1 and 0x00FF00) * f1) + ((c2 and 0x00FF00) * factor)) and 0x00FF0000))) ushr 8
	}

	@JvmStatic operator fun invoke(r: Int, g: Int, b: Int, a: Int) = pack(r, g, b, a)

	@JvmStatic fun rgbaToBgra(v: Int) = ((v shl 16) and 0x00FF0000) or ((v shr 16) and 0x000000FF) or (v and 0xFF00FF00.toInt())

	@JvmStatic private fun f2i(v: Float): Int = (ColorFormat.clampf01(v) * 255).toInt()

	@JvmStatic fun packf(r: Float, g: Float, b: Float, a: Float): Int = packFast(f2i(r), f2i(g), f2i(b), f2i(a))
	@JvmStatic fun packf(rgb: Int, a: Float): Int = packRGB_A(rgb, f2i(a))

	@JvmStatic fun mix(dst: Int, src: Int): Int {
		val a = RGBA.getA(src)
		return when (a) {
			0x000 -> dst
			0xFF -> src
			else -> {
				RGBA.packRGB_A(
						RGBA.blend(dst, src, a * 256 / 255),
						RGBA.clampFF(RGBA.getA(dst) + RGBA.getA(src))
				)
			}
		}
	}
}