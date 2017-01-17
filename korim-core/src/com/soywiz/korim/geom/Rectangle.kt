package com.soywiz.korim.geom

data class Rectangle(
	var x: Double = 0.0, var y: Double = 0.0,
	var width: Double = 0.0, var height: Double = 0.0
) {
	var left: Double; get() = x; set(value) = run { x = value }
	var top: Double; get() = y; set(value) = run { y = value }
	var right: Double; get() = x + width; set(value) = run { width = value - x }
	var bottom: Double; get() = y + height; set(value) = run { height = value - y }

	fun setTo(x: Double, y: Double, width: Double, height: Double) = this.apply {
		this.x = x
		this.y = y
		this.width = width
		this.height = height
	}

	fun setBounds(left: Double, top: Double, right: Double, bottom: Double) = setTo(left, top, right - left, bottom - top)
}