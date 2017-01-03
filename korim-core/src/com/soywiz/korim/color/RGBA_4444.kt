package com.soywiz.korim.color

object RGBA_4444 : ColorFormat() {
	override fun getR(v: Int): Int = (((v ushr 0) and 0xF) * 0xFF) / 0xF
	override fun getG(v: Int): Int = (((v ushr 4) and 0xF) * 0xFF) / 0xF
	override fun getB(v: Int): Int = (((v ushr 8) and 0xF) * 0xFF) / 0xF
	override fun getA(v: Int): Int = (((v ushr 12) and 0xF) * 0xFF) / 0xF

	fun packComponent(v: Int): Int = (v * 0xF) / 0xFF

	override fun pack(r: Int, g: Int, b: Int, a: Int): Int {
		return (packComponent(r) shl 0) or
			(packComponent(g) shl 4) or
			(packComponent(b) shl 8) or
			(packComponent(a) shl 12)
	}
}