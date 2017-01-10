package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors

class Bitmap32Context2d(val bmp: Bitmap32) : Context2d() {
	/*
	var color = Colors.RED

	override fun lineInternal(x0: Double, y0: Double, x1: Double, y1: Double) {
		plotLine(x0.toInt(), y0.toInt(), x1.toInt(), y1.toInt(), color)
	}

	fun plotLine(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
		val dx = x1 - x0
		val dy = y1 - y0
		var D = 2 * dy - dx
		var y = y0

		for (x in x0..x1) {
			bmp[x, y] = color
			if (D > 0) {
				y++
				D -= dx
			}
			D += dy
		}
	}
	*/
}