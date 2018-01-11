package com.soywiz.korim.color

import com.soywiz.kmem.extract8
import com.soywiz.korio.JvmStatic

// https://en.wikipedia.org/wiki/YCbCr
object YCbCr : ColorFormat32() {
	fun getY(v: Int): Int = v.extract8(0) // Luma
	fun getCb(v: Int): Int = v.extract8(8) // Chrominance1
	fun getCr(v: Int): Int = v.extract8(16)// Chrominance2

	override fun getR(v: Int): Int = getY(v)
	override fun getG(v: Int): Int = getCb(v)
	override fun getB(v: Int): Int = getCr(v)
	override fun getA(v: Int): Int = v.extract8(24)

	override fun pack(r: Int, g: Int, b: Int, a: Int): Int = RGBA.pack(r, g, b, a)

	@JvmStatic
	fun getY(r: Int, g: Int, b: Int): Int = clamp0_FF((0 + (0.299 * r) + (0.587 * g) + (0.114 * b)).toInt())

	@JvmStatic
	fun getCb(r: Int, g: Int, b: Int): Int = clamp0_FF((128 - (0.168736 * r) - (0.331264 * g) + (0.5 * b)).toInt())

	@JvmStatic
	fun getCr(r: Int, g: Int, b: Int): Int = clamp0_FF((128 + (0.5 * r) - (0.418688 * g) - (0.081312 * b)).toInt())


	@JvmStatic
	fun getR(y: Int, cb: Int, cr: Int): Int = clamp0_FF((y + 1.402 * (cr - 128)).toInt())

	@JvmStatic
	fun getG(y: Int, cb: Int, cr: Int): Int = clamp0_FF((y - 0.34414 * (cb - 128) - 0.71414 * (cr - 128)).toInt())

	@JvmStatic
	fun getB(y: Int, cb: Int, cr: Int): Int = clamp0_FF((y + 1.772 * (cb - 128)).toInt())

	fun rgbaToYCbCr(c: Int): Int {
		val R = RGBA.getR(c)
		val G = RGBA.getG(c)
		val B = RGBA.getB(c)
		val A = RGBA.getA(c)

		val Y = getY(R, G, B)
		val Cb = getCb(R, G, B)
		val Cr = getCr(R, G, B)

		return RGBA.pack(Y, Cb, Cr, A)
	}

	fun yCbCrToRgba(c: Int): Int {
		val Y = RGBA.getR(c)
		val Cb = RGBA.getG(c)
		val Cr = RGBA.getB(c)
		val A = RGBA.getA(c)

		val R = getR(Y, Cb, Cr)
		val G = getG(Y, Cb, Cr)
		val B = getB(Y, Cb, Cr)

		return RGBA.pack(R, G, B, A)
	}
}
