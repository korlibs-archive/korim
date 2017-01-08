package com.soywiz.korim.color

abstract class ColorFormat32 : ColorFormat(32) {
	open fun encode(colors: IntArray, colorsOffset: Int, out: IntArray, outOffset: Int, size: Int): Unit {
		var io = colorsOffset
		var oo = outOffset
		for (n in 0 until size) {
			val c = colors[io++]
			out[oo++] = pack(RGBA.getR(c), RGBA.getG(c), RGBA.getB(c), RGBA.getA(c))
		}
	}
}