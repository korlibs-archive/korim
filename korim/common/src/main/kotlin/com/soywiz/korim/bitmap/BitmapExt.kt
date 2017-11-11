package com.soywiz.korim.bitmap

import com.soywiz.korma.geom.RectangleInt

fun <T : Bitmap> T.sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int): BitmapSlice<T> = BitmapSlice<T>(this, RectangleInt(left, top, right - left, bottom - top))
fun <T : Bitmap> T.sliceWithSize(x: Int, y: Int, width: Int, height: Int): BitmapSlice<T> = BitmapSlice<T>(this, RectangleInt(x, y, width, height))
fun <T : Bitmap> T.slice(bounds: RectangleInt): BitmapSlice<T> = BitmapSlice<T>(this, bounds)

fun Bitmap.matchContents(that: Bitmap): Boolean {
	if (this.width != that.width || this.height != that.height) return false
	val l = this.toBMP32().depremultipliedIfRequired()
	val r = that.toBMP32().depremultipliedIfRequired()
	val width = l.width
	val height = l.height
	for (y in 0 until height) {
		for (x in 0 until width) {
			if (l.get32(x, y) != r.get32(x, y)) return false
		}
	}
	return true
}

fun Bitmap32.setAlpha(value: Int) {
	for (n in 0 until this.data.size) this.data[n] = (this.data[n] and 0x00FFFFFF) or (value shl 24)
}
