package com.soywiz.korim.geom

class IRect(val x: Int, val y: Int, val width: Int, val height: Int) {
	val left: Int = x
	val top: Int = y
	val right: Int = x + width
	val bottom: Int = y + height
}