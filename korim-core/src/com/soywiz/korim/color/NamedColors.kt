package com.soywiz.korim.color

object NamedColors {
	val colorsByName = mapOf(
		"black" to RGBA(0, 0, 0, 0xFF),
		"white" to RGBA(0xFF, 0xFF, 0xFF, 0xFF),
		"red" to RGBA(0xFF, 0, 0, 0xFF),
		"green" to RGBA(0, 0x80, 0, 0xFF),
		"blue" to RGBA(0, 0, 0xFF, 0xFF),
		"lime" to RGBA(0, 0xFF, 0, 0xFF),
		"orange" to RGBA(0xFF, 0xA5, 0x00, 0xFF),
		"pink" to RGBA(0xFF, 0xC0, 0xCB, 0xFF)
	)

	operator fun get(str: String, default: Int = Colors.RED): Int = colorsByName[str.toLowerCase()] ?: default

	fun toHtmlString(color: Int) = "RGBA(" + RGBA.getR(color) + "," + RGBA.getG(color) + "," + RGBA.getB(color) + "," + RGBA.getAf(color) + ")"
}