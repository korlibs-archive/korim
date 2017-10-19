package com.soywiz.korim.bitmap

class Bitmap2(
	width: Int,
	height: Int,
	data: ByteArray = ByteArray(width * height / 4),
	palette: IntArray = IntArray(4)
) : BitmapIndexed(2, width, height, data, palette) {
	override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap2(width, height)
}