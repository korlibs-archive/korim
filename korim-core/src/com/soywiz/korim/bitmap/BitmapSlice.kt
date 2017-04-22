package com.soywiz.korim.bitmap

import com.soywiz.korma.geom.RectangleInt

class BitmapSlice<out T : Bitmap>(val bmp: T, val bounds: RectangleInt) {
	fun extract(): T {
		val sx = bounds.x
		val sy = bounds.y
		val swidth = bounds.width
		val sheight = bounds.height
		val out = bmp.createWithThisFormatTyped(swidth, sheight)
		for (y in 0 until sheight) {
			for (x in 0 until swidth) {
				out[x, y] = bmp[sx + x, sy + y]
			}
		}
		return out
	}
}