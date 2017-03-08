package com.soywiz.korim.bitmap

class Bitmap1(
	width: Int,
	height: Int,
	data: ByteArray = ByteArray(width * height / 8),
	palette: IntArray = IntArray(2)
) : BitmapIndexed(1, width, height, data, palette) {
	override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap1(width, height)
}