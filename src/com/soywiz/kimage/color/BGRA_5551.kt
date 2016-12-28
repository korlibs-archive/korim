package com.soywiz.kimage.color

object BGRA_5551 : ColorFormat() {
	override fun getB(v: Int): Int = (((v ushr 0) and 0x1F) * 0xFF) / 0x1F
	override fun getG(v: Int): Int = (((v ushr 5) and 0x1F) * 0xFF) / 0x1F
	override fun getR(v: Int): Int = (((v ushr 10) and 0x1F) * 0xFF) / 0x1F
	override fun getA(v: Int): Int = 255 - ((((v ushr 15) and 1) * 0xFF) / 1)
}