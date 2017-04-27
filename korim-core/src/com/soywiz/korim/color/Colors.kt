package com.soywiz.korim.color

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.isEven
import com.soywiz.korio.util.isOdd
import com.soywiz.korio.util.substr

object Colors {
	@JvmStatic val WHITE = RGBA.packFast(0xFF, 0xFF, 0xFF, 0xFF)
	@JvmStatic val BLACK = RGBA.packFast(0x00, 0x00, 0x00, 0xFF)
	@JvmStatic val RED = RGBA.packFast(0xFF, 0x00, 0x00, 0xFF)
	@JvmStatic val GREEN = RGBA.packFast(0x00, 0xFF, 0x00, 0xFF)
	@JvmStatic val BLUE = RGBA.packFast(0x00, 0x00, 0xFF, 0xFF)

	@JvmStatic val TRANSPARENT_BLACK = RGBA.packFast(0x00, 0x00, 0x00, 0x00)
	@JvmStatic val TRANSPARENT_WHITE = RGBA.packFast(0x00, 0x00, 0x00, 0x00)

	operator fun get(s: String): Int {
		if (s.startsWith("#")) {
			val ss = s.substr(1)
			val N = if (ss.length >= 6) 2 else 1
			val comps = ss.length / N
			val scale = if (N == 1) 1.0 / 15.0 else 1.0 / 255.0
			val rf = ss.substr(N * 0, N).toInt(16) * scale
			val gf = ss.substr(N * 1, N).toInt(16) * scale
			val bf = ss.substr(N * 2, N).toInt(16) * scale
			val af = if (comps >= 4) ss.substr(N * 3, N).toInt(16) * scale else 1.0
			return RGBA.packf(rf, gf, bf, af)
		} else {
			invalidOp("Unsupported color $s")
		}
	}
}