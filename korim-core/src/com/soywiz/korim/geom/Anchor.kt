package com.soywiz.korim.geom

data class Anchor(val sx: Double, val sy: Double) {
	companion object {
		val TOP_LEFT = Anchor(0.0, 0.0)
		val TOP_CENTER = Anchor(0.5, 0.0)
		val TOP_RIGHT = Anchor(1.0, 0.0)

		val MIDDLE_LEFT = Anchor(0.0, 0.5)
		val MIDDLE_CENTER = Anchor(0.5, 0.5)
		val MIDDLE_RIGHT = Anchor(1.0, 0.5)

		val BOTTOM_LEFT = Anchor(0.0, 1.0)
		val BOTTOM_CENTER = Anchor(0.5, 1.0)
		val BOTTOM_RIGHT = Anchor(1.0, 1.0)
	}
}