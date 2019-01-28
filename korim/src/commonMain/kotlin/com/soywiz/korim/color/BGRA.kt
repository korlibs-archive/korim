package com.soywiz.korim.color

object BGRA : ColorFormat by ColorFormat.Mixin(
    32,
	bOffset = 0, bSize = 8,
	gOffset = 8, gSize = 8,
	rOffset = 16, rSize = 8,
	aOffset = 24, aSize = 8
) {
    fun rgbaToBgra(v: Int): Int = ((v shl 16) and 0x00FF0000) or ((v shr 16) and 0x000000FF) or (v and 0xFF00FF00.toInt())
}
