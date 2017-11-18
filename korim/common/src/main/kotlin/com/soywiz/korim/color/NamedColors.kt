package com.soywiz.korim.color

import com.soywiz.korio.lang.format
import com.soywiz.korio.util.substr

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

	operator fun get(str: String, default: Int = Colors.RED): Int {
		if (str.startsWith("#")) {
			val hex = str.substr(1)
			val r: Int
			val g: Int
			val b: Int
			val a: Int
			when (hex.length) {
				3 -> {
					r = (hex.substr(0, 1).toInt(0x10) * 255) / 15
					g = (hex.substr(1, 1).toInt(0x10) * 255) / 15
					b = (hex.substr(2, 1).toInt(0x10) * 255) / 15
					a = 255
				}
				6 -> {
					r = hex.substr(0, 2).toInt(0x10)
					g = hex.substr(2, 2).toInt(0x10)
					b = hex.substr(4, 2).toInt(0x10)
					a = 255
				}
				else -> {
					r = 0; g = 0; b = 0; a = 0xFF
				}
			}
			return RGBA.pack(r, g, b, a)
		} else {
			return colorsByName[str.toLowerCase()] ?: default
		}
	}

	fun toHtmlString(color: Int) = "RGBA(" + RGBA.getR(color) + "," + RGBA.getG(color) + "," + RGBA.getB(color) + "," + RGBA.getAf(color) + ")"
	fun toHtmlStringSimple(color: Int) = "#%02x%02x%02x".format(RGBA.getR(color), RGBA.getG(color), RGBA.getB(color))
}