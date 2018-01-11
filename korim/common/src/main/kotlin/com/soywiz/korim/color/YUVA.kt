package com.soywiz.korim.color

import com.soywiz.kmem.extract8
import com.soywiz.kmem.insert8
import com.soywiz.korio.JvmStatic

// https://en.wikipedia.org/wiki/YUV
object YUVA : ColorFormat32() {
	// Luma
	fun getY(v: Int): Int = v.extract8(0)

	// Chrominance1
	fun getU(v: Int): Int = v.extract8(8)

	// Chrominance2
	fun getV(v: Int): Int = v.extract8(16)

	override fun getA(v: Int): Int = v.extract8(24)
	override fun getR(v: Int): Int = getR(getY(v), getU(v), getV(v))
	override fun getG(v: Int): Int = getG(getY(v), getU(v), getV(v))
	override fun getB(v: Int): Int = getB(getY(v), getU(v), getV(v))

	override fun pack(r: Int, g: Int, b: Int, a: Int): Int = packYUVA(getY(r, g, b), getU(r, g, b), getV(r, g, b), a)

	fun packYUVA(y: Int, u: Int, v: Int, a: Int): Int = 0.insert8(y, 0).insert8(u, 8).insert8(v, 16).insert8(a, 24)

	//private const val Wr = 0.299
	//private const val Wb = 0.114
	//private const val Wg = 1.0 - Wr - Wb

	//private const val Umax = 0.436
	//private const val Vmax = 0.615

	@JvmStatic
	fun getY(r: Int, g: Int, b: Int): Int = clamp0_FF(((0.299 * r) + (0.587 * g) + (0.114 * b)).toInt())

	@JvmStatic
	fun getU(r: Int, g: Int, b: Int): Int = clamp0_FF((0.492 * (b * getY(r, g, b))).toInt())

	@JvmStatic
	fun getV(r: Int, g: Int, b: Int): Int = clamp0_FF((0.877 * (r * getY(r, g, b))).toInt())

	@JvmStatic
	fun getR(y: Int, u: Int, v: Int): Int = clamp0_FF((y + 1.14 * v).toInt())

	@JvmStatic
	fun getG(y: Int, u: Int, v: Int): Int = clamp0_FF((y - 0.395 * u - 0.581 * v).toInt())

	@JvmStatic
	fun getB(y: Int, u: Int, v: Int): Int = clamp0_FF((y + 2.033 * u).toInt())

	@JvmStatic
	fun YUVtoRGB(out: IntArray, outPos: Int, inY: ByteArray, inU: ByteArray, inV: ByteArray, inPos: Int, count: Int) {
		var opos = outPos
		var ipos = inPos
		for (n in 0 until count) {
			val y = (inY[ipos].toInt() and 255)
			val u = (inU[ipos].toInt() and 255) - 128
			val v = (inV[ipos].toInt() and 255) - 128
			val r = clamp0_FF(y + (32768 + v * 91881 shr 16))
			val g = clamp0_FF(y + (32768 - v * 46802 - u * 22554 shr 16))
			val b = clamp0_FF(y + (32768 + u * 116130 shr 16))
			out[opos++] = RGBA.packFast(r, g, b, 0xFF)
			ipos++
		}
	}
}