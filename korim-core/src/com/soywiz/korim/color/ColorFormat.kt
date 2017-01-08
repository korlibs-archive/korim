package com.soywiz.korim.color

abstract class ColorFormat {
	abstract fun getR(v: Int): Int
	abstract fun getG(v: Int): Int
	abstract fun getB(v: Int): Int
	abstract fun getA(v: Int): Int

	fun getRf(v: Int): Float = getR(v).toFloat() / 255f
	fun getGf(v: Int): Float = getG(v).toFloat() / 255f
	fun getBf(v: Int): Float = getB(v).toFloat() / 255f
	fun getAf(v: Int): Float = getA(v).toFloat() / 255f

	fun getRd(v: Int): Double = getR(v).toDouble() / 255.0
	fun getGd(v: Int): Double = getG(v).toDouble() / 255.0
	fun getBd(v: Int): Double = getB(v).toDouble() / 255.0
	fun getAd(v: Int): Double = getA(v).toDouble() / 255.0

	fun clamp0_FF(a: Int): Int = Math.min(Math.max(a, 0), 255)
	fun clampFF(a: Int): Int = Math.min(a, 255)

	fun toRGBA(v: Int) = RGBA.packFast(getR(v), getG(v), getB(v), getA(v))

	open fun pack(r: Int, g: Int, b: Int, a: Int): Int = TODO()

	fun packRGBA(v: Int): Int = pack(RGBA.getR(v), RGBA.getG(v), RGBA.getB(v), RGBA.getA(v))

	fun convertTo(color: Int, target: ColorFormat): Int = target.pack(
		this.getR(color), this.getG(color), this.getB(color), this.getA(color)
	)

	companion object {
		fun clamp0_FF(v: Int) = if (v < 0x00) 0x00 else if (v > 0xFF) 0xFF else v
		fun clampf01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v
	}
}