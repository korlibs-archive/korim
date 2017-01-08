package com.soywiz.korim.color

import com.soywiz.korio.util.extract8

object BGRA : ColorFormat() {
	override fun getB(v: Int): Int = v.extract8(0)
	override fun getG(v: Int): Int = v.extract8(8)
	override fun getR(v: Int): Int = v.extract8(16)
	override fun getA(v: Int): Int = v.extract8(24)
}