package com.soywiz.korim.color

import com.soywiz.korio.*

class RGBAd(
	@JvmField var r: Double,
	@JvmField var g: Double,
	@JvmField var b: Double,
	@JvmField var a: Double
) {
	constructor(c: RGBAd) : this(c.r, c.g, c.b, c.a)
	constructor(c: Int) : this(RGBA.getRd(c), RGBA.getGd(c), RGBA.getBd(c), RGBA.getAd(c))
	constructor() : this(0.0, 0.0, 0.0, 0.0)

	fun set(r: Double, g: Double, b: Double, a: Double) {
		this.r = r
		this.g = g
		this.b = b
		this.a = a
	}

	fun add(r: Double, g: Double, b: Double, a: Double) {
		this.r += r
		this.g += g
		this.b += b
		this.a += a
	}

	fun set(c: RGBAd) = set(c.r, c.g, c.b, c.a)
}
