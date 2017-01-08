package com.soywiz.korim.color

class RGBAf(
	_r: Float = 1f,
	_g: Float = 1f,
	_b: Float = 1f,
	_a: Float = 1f
) {
	private var _r: Float = r
	private var _g: Float = g
	private var _b: Float = b
	private var _a: Float = a

	var r: Float; set(v) = run { _r = v; updateColor() }; get() = _r
	var g: Float; set(v) = run { _g = v; updateColor() }; get() = _g
	var b: Float; set(v) = run { _b = v; updateColor() }; get() = _b
	var a: Float; set(v) = run { _a = v; updateColor() }; get() = _a

	private fun updateColor() {
		rgba = RGBA.packfFast(_r, _g, _b, _a)
	}

	var rgba: Int = -1; private set

	fun setTo(r: Float, g: Float, b: Float, a: Float) {
		this._r = r
		this._g = g
		this._b = b
		this._a = a
		updateColor()
	}

	fun copyFrom(that: RGBAf) = setTo(that.r, that.g, that.b, that.a)
	fun setToMultiply(that: RGBAf) = setToMultiply(that.r, that.g, that.b, that.a)
	fun setToMultiply(r: Float, g: Float, b: Float, a: Float) = setTo(this.r * r, this.g * g, this.b * b, this.a * a)

	fun toRGBA(): Int = RGBA.packFast((r * 255).toInt() and 0xFF, (g * 255).toInt() and 0xFF, (b * 255).toInt() and 0xFF, (a * 255).toInt() and 0xFF)

	fun setToIdentity() = setTo(1f, 1f, 1f, 1f)
}
