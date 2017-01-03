package com.soywiz.korim.bitmap

enum class BitmapChannel(val index: Int) {
	RED(0), GREEN(1), BLUE(2), ALPHA(3);

	val shift = index * 8
	val setMask = (0xFF shl shift)
	val clearMask = setMask.inv()
}
