package com.soywiz.korim.geom

data class Vector2(var x: Double = 0.0, var y: Double = x) {
	constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

	fun setTo(x: Double, y: Double): Vector2 {
		this.x = x
		this.y = y
		return this
	}

	fun copyFrom(that: Vector2) = setTo(that.x, that.y)

	fun setToTransform(mat: Matrix2d, p: Vector2): Vector2 = setToTransform(mat, p.x, p.y)

	fun setToTransform(mat: Matrix2d, x: Double, y: Double): Vector2 = setTo(
		mat.transformX(x, y),
		mat.transformY(x, y)
	)

	fun setToAdd(a: Vector2, b: Vector2): Vector2 = setTo(
		a.x + b.x,
		a.y + b.y
	)

	operator fun plusAssign(that: Vector2) {
		setTo(this.x + that.x, this.y + that.y)
	}

	val unit: Vector2 get() = this / length
	val length: Double get() = Math.hypot(x, y)
	operator fun plus(that: Vector2) = Vector2(this.x + that.x, this.y + that.y)
	operator fun minus(that: Vector2) = Vector2(this.x - that.x, this.y - that.y)
	operator fun times(that: Vector2) = this.x * that.x + this.y * that.y
	operator fun times(v: Double) = Vector2(x * v, y * v)
	operator fun div(v: Double) = Vector2(x / v, y / v)

	companion object {
		fun angle(a: Vector2, b: Vector2): Double {
			return Math.acos((a * b) / (a.length * b.length))
		}

		fun angle(ax: Double, ay: Double, bx: Double, by: Double): Double {
			return Math.acos(((ax * bx) + (ay * by)) / (Math.hypot(ax, ay) * Math.hypot(bx, by)))
		}

		fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Double {
			val ax = x1 - x2
			val ay = y1 - y2
			val al = Math.hypot(ax, ay)

			val bx = x1 - x3
			val by = y1 - y3
			val bl = Math.hypot(bx, by)

			return Math.acos((ax * bx + ay * by) / (al * bl))
		}
	}

	override fun toString(): String = "Vector2($x, $y)"
}