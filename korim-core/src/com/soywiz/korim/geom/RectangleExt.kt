package com.soywiz.korim.geom

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors

fun Iterable<Rectangle>.bounds(target: Rectangle = Rectangle()): Rectangle {
	var first = true
	var left = 0.0
	var right = 0.0
	var top = 0.0
	var bottom = 0.0
	for (r in this) {
		if (first) {
			left = r.left
			right = r.right
			top = r.top
			bottom = r.bottom
			first = false
		} else {
			left = Math.min(left, r.left)
			right = Math.max(right, r.right)
			top = Math.min(top, r.top)
			bottom = Math.max(bottom, r.bottom)
		}
	}
	return target.setBounds(left, top, right, bottom)
}

fun Iterable<Rectangle>.render(): Bitmap32 {
	val colors = listOf(Colors.RED, Colors.GREEN, Colors.BLUE, Colors.BLACK)
	val bounds = this.bounds()
	val out = Bitmap32(bounds.width.toInt(), bounds.height.toInt())
	for ((index, r) in this.withIndex()) {
		val color = colors[index % colors.size]
		out.fill(color, r.x.toInt(), r.y.toInt(), r.width.toInt(), r.height.toInt())
	}
	return out
}