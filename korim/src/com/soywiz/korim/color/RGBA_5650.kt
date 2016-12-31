package com.soywiz.korim.color

object RGBA_5650 : ColorFormat() {
	override fun getR(v: Int): Int = (((v ushr 0) and 0x1F) * 0xFF) / 0x1F
	override fun getG(v: Int): Int = (((v ushr 5) and 0x3F) * 0xFF) / 0x3F
	override fun getB(v: Int): Int = (((v ushr 11) and 0x1F) * 0xFF) / 0x1F
	override fun getA(v: Int): Int = 0xFF
}