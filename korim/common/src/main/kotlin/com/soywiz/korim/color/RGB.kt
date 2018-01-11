package com.soywiz.korim.color

import com.soywiz.kmem.extract8
import com.soywiz.kmem.insert

object RGB : ColorFormat24() {
	override fun getR(v: Int): Int = v.extract8(0)
	override fun getG(v: Int): Int = v.extract8(8)
	override fun getB(v: Int): Int = v.extract8(16)
	override fun getA(v: Int): Int = 0xFF

	override fun pack(r: Int, g: Int, b: Int, a: Int): Int = 0.insert(r, 0, 8).insert(g, 8, 8).insert(b, 16, 8)
}

object BGR : ColorFormat24() {
	override fun getR(v: Int): Int = v.extract8(16)
	override fun getG(v: Int): Int = v.extract8(8)
	override fun getB(v: Int): Int = v.extract8(0)
	override fun getA(v: Int): Int = 0xFF

	override fun pack(r: Int, g: Int, b: Int, a: Int): Int = 0.insert(r, 16, 8).insert(g, 8, 8).insert(b, 0, 8)
}