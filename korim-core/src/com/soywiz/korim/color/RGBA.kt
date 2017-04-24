package com.soywiz.korim.color

import com.soywiz.korim.color.RGBA.blendRGB

object RGBA : ColorFormat32() {
	//private inline val R_SHIFT: Int get() = 0
	//private inline val G_SHIFT: Int get() = 8
	//private inline val B_SHIFT: Int get() = 16
	//private inline val A_SHIFT: Int get() = 24

	override fun getR(v: Int): Int = getFastR(v)
	override fun getG(v: Int): Int = getFastG(v)
	override fun getB(v: Int): Int = getFastB(v)
	override fun getA(v: Int): Int = getFastA(v)

	override fun pack(r: Int, g: Int, b: Int, a: Int) = ((ColorFormat.clamp0_FF(r)) shl 0) or ((ColorFormat.clamp0_FF(g)) shl 8) or ((ColorFormat.clamp0_FF(b)) shl 16) or ((ColorFormat.clamp0_FF(a)) shl 24)

	@JvmStatic fun getFastR(v: Int): Int = (v ushr 0) and 0xFF
	@JvmStatic fun getFastG(v: Int): Int = (v ushr 8) and 0xFF
	@JvmStatic fun getFastB(v: Int): Int = (v ushr 16) and 0xFF
	@JvmStatic fun getFastA(v: Int): Int = (v ushr 24) and 0xFF

	@JvmStatic fun getFastRf(v: Int): Float = ((v ushr 0) and 0xFF).toFloat() / 0xFF
	@JvmStatic fun getFastGf(v: Int): Float = ((v ushr 8) and 0xFF).toFloat() / 0xFF
	@JvmStatic fun getFastBf(v: Int): Float = ((v ushr 16) and 0xFF).toFloat() / 0xFF
	@JvmStatic fun getFastAf(v: Int): Float = ((v ushr 24) and 0xFF).toFloat() / 0xFF

	@JvmStatic fun getFastRd(v: Int): Double = ((v ushr 0) and 0xFF).toDouble() / 0xFF
	@JvmStatic fun getFastGd(v: Int): Double = ((v ushr 8) and 0xFF).toDouble() / 0xFF
	@JvmStatic fun getFastBd(v: Int): Double = ((v ushr 16) and 0xFF).toDouble() / 0xFF
	@JvmStatic fun getFastAd(v: Int): Double = ((v ushr 24) and 0xFF).toDouble() / 0xFF

	//fun getRGB(v: Int): Int = v and 0xFFFFFF

	@JvmStatic fun getRGB(v: Int): Int = v and 0xFFFFFF

	@Deprecated("", ReplaceWith("RGBA.premultiplyFast(v)", "com.soywiz.korim.color.RGBA"))
	@JvmStatic fun multipliedByAlpha(v: Int): Int = premultiplyFast(v)

	@JvmStatic fun toHexString(v: Int): String = "#%02x%02x%02x%02x".format(getFastR(v), getFastG(v), getFastB(v), getFastA(v))

	@JvmStatic fun toHtmlColor(v: Int): String = "rgba(${getFastR(v)}, ${getFastG(v)}, ${getFastB(v)}, ${getFastAf(v)})"

	@JvmStatic fun premultiply(v: Int): Int = premultiplyFast(v)

	@JvmStatic fun premultiplyAccurate(v: Int): Int {
		val a1 = getFastA(v)
		val af = a1.toDouble() / 255.0
		return packFast((getFastR(v) * af).toInt(), (getFastG(v) * af).toInt(), (getFastB(v) * af).toInt(), a1)
	}

	@JvmStatic fun premultiplyFast(v: Int): Int {
		val A = (v ushr 24) + 1
		val RB = (((v and 0x00FF00FF) * A) ushr 8) and 0x00FF00FF
		val G = (((v and 0x0000FF00) * A) ushr 8) and 0x0000FF00
		return (v and 0x00FFFFFF.inv()) or RB or G
	}

	//@JvmStatic fun premultiplyFast2(v: Int): Int {
	//	val Ad = (v ushr 24).toDouble() / 255.0
	//	val RB = ((v and 0x00FF00FF) * Ad).toInt() and 0x00FF00FF
	//	val G = ((v and 0x0000FF00) * Ad).toInt() and 0x0000FF00
	//	return (v and 0x00FFFFFF.inv()) or RB or G
	//}

	@JvmStatic fun mutliplyByAlpha(v: Int, alpha: Double): Int = RGBA.pack(getFastR(v), getFastG(v), getFastB(v), (getFastA(v) * alpha).toInt())

	@JvmStatic fun depremultiply(v: Int): Int {
		val alpha = getAd(v)
		if (alpha == 0.0) {
			return Colors.TRANSPARENT_WHITE
		} else {
			return pack((getFastR(v) / alpha).toInt(), (getFastG(v) / alpha).toInt(), (getFastB(v) / alpha).toInt(), getFastA(v))
		}
	}

	@JvmStatic fun packFast(r: Int, g: Int, b: Int, a: Int) = (r shl 0) or (g shl 8) or (b shl 16) or (a shl 24)
	@JvmStatic fun packfFast(r: Float, g: Float, b: Float, a: Float): Int = ((r * 0xFF).toInt() shl 0) or ((g * 0xFF).toInt() shl 8) or ((b * 0xFF).toInt() shl 16) or ((a * 0xFF).toInt() shl 24)

	@JvmStatic fun packRGB_A(rgb: Int, a: Int): Int = (rgb and 0xFFFFFF) or (a shl 24)

	@JvmStatic fun blendComponent(c1: Int, c2: Int, factor: Double): Int = (c1 * (1.0 - factor) + c2 * factor).toInt() and 0xFF

	@JvmStatic fun blendRGB(c1: Int, c2: Int, factor256: Int): Int {
		val f1 = 256 - factor256
		return ((
			((((c1 and 0xFF00FF) * f1) + ((c2 and 0xFF00FF) * factor256)) and 0xFF00FF00.toInt())
				or
				((((c1 and 0x00FF00) * f1) + ((c2 and 0x00FF00) * factor256)) and 0x00FF0000))) ushr 8
	}

	@Deprecated("", ReplaceWith("blendRGB(c1, c2, factor)", "com.soywiz.korim.color.RGBA.blendRGB"))
	@JvmStatic fun blend(c1: Int, c2: Int, factor: Int): Int = blendRGB(c1, c2, factor)

	@Deprecated("", ReplaceWith("blendRGB(c1, c2, factor)", "com.soywiz.korim.color.RGBA.blendRGB"))
	@JvmStatic fun blend(c1: Int, c2: Int, factor: Double): Int = blendRGB(c1, c2, factor)

	@JvmStatic fun blendRGB(c1: Int, c2: Int, factor: Double): Int = blendRGB(c1, c2, (factor * 256).toInt())

	@JvmStatic fun blendRGBA(c1: Int, c2: Int, factor: Double): Int {
		val RGB = blendRGB(c1 and 0xFFFFFF, c2 and 0xFFFFFF, (factor * 256).toInt())
		val A = blendComponent(getFastA(c1), getFastA(c2), factor)
		return packRGB_A(RGB, A)
	}

	@JvmStatic operator fun invoke(r: Int, g: Int, b: Int, a: Int) = pack(r, g, b, a)

	@JvmStatic fun rgbaToBgra(v: Int) = ((v shl 16) and 0x00FF0000) or ((v shr 16) and 0x000000FF) or (v and 0xFF00FF00.toInt())

	@JvmStatic private fun f2i(v: Float): Int = (ColorFormat.clampf01(v) * 255).toInt()

	@JvmStatic fun packf(r: Float, g: Float, b: Float, a: Float): Int = packFast(f2i(r), f2i(g), f2i(b), f2i(a))
	@JvmStatic fun packf(rgb: Int, a: Float): Int = packRGB_A(rgb, f2i(a))

	@JvmStatic fun mix(dst: Int, src: Int): Int {
		val srcA = RGBA.getFastA(src)
		return when (srcA) {
			0x000 -> dst
			0xFF -> src
			else -> {
				RGBA.packRGB_A(
					blendRGB(dst, src, srcA + 1),
					clampFF(RGBA.getFastA(dst) + srcA)
				)
			}
		}
	}

	@JvmStatic fun multiply(c1: Int, c2: Int): Int {
		return RGBA.pack(
			(RGBA.getFastR(c1) * RGBA.getFastR(c2)) / 0xFF,
			(RGBA.getFastG(c1) * RGBA.getFastG(c2)) / 0xFF,
			(RGBA.getFastB(c1) * RGBA.getFastB(c2)) / 0xFF,
			(RGBA.getFastA(c1) * RGBA.getFastA(c2)) / 0xFF
		)
	}
}