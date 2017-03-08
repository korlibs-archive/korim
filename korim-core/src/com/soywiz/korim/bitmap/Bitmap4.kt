package com.soywiz.korim.bitmap

class Bitmap4(
	width: Int,
	height: Int,
	data: ByteArray = ByteArray(width * height / 2),
	palette: IntArray = IntArray(16)
) : BitmapIndexed(4, width, height, data, palette) {
	override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap4(width, height)
}
