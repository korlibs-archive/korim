package com.soywiz.korim.geom

class Point2d(var x: Double = 0.0, var y: Double = x) {
	fun setTo(x: Double, y: Double): Point2d {
		this.x = x
		this.y = y
		return this
	}

	fun copyFrom(that: Point2d) = setTo(that.x, that.y)

	fun setToTransform(mat: Matrix2d, p: Point2d): Point2d = setToTransform(mat, p.x, p.y)

	fun setToTransform(mat: Matrix2d, x: Double, y: Double): Point2d = setTo(
		mat.transformX(x, y),
		mat.transformY(x, y)
	)

	fun setToAdd(a: Point2d, b: Point2d): Point2d = setTo(
		a.x + b.x,
		a.y + b.y
	)

	operator fun plusAssign(that: Point2d) {
		setTo(this.x + that.x, this.y + that.y)
	}
}