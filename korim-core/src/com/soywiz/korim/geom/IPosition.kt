package com.soywiz.korim.geom

data class IPosition(var x: Int = 0, var y: Int = x) {
	fun setTo(x: Int, y: Int) = this.apply { this.x = x; this.y = y }
	fun setTo(that: IPosition) = this.setTo(that.x, that.y)
}