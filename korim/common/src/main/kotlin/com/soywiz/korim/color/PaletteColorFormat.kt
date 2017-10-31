package com.soywiz.korim.color

class PaletteColorFormat(val palette: IntArray) : ColorFormat(8) {
	override fun getR(v: Int): Int = RGBA.getFastR(palette[v])
	override fun getG(v: Int): Int = RGBA.getFastG(palette[v])
	override fun getB(v: Int): Int = RGBA.getFastB(palette[v])
	override fun getA(v: Int): Int = RGBA.getFastA(palette[v])

	override fun pack(r: Int, g: Int, b: Int, a: Int): Int {
		TODO("Not implemented. Must locate best color in palette")
	}
}