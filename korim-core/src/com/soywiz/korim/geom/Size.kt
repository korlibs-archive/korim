package com.soywiz.korim.geom

class Size(var width: Double, var height: Double) {
	constructor(width: Int, height: Int) : this(width.toDouble(), height.toDouble())

	val area: Double get() = width * height
	val perimeter: Double get() = width * 2 + height * 2
	val min: Double get() = Math.min(width, height)
	val max: Double get() = Math.max(width, height)

	fun clone() = Size(width, height)
}
