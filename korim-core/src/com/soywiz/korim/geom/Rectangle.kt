package com.soywiz.korim.geom

data class Rectangle(var x: Double = 0.0, var y: Double = 0.0, var width: Double = 0.0, var height: Double = 0.0) {
	constructor(x: Int, y: Int, width: Int, height: Int) : this(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

	var left: Double; get() = x; set(value) = run { x = value }
	var top: Double; get() = y; set(value) = run { y = value }
	var right: Double; get() = x + width; set(value) = run { width = value - x }
	var bottom: Double; get() = y + height; set(value) = run { height = value - y }

	fun setTo(x: Int, y: Int, width: Int, height: Int) = this.setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

	fun setTo(x: Double, y: Double, width: Double, height: Double) = this.apply {
		this.x = x
		this.y = y
		this.width = width
		this.height = height
	}

	fun setBounds(left: Double, top: Double, right: Double, bottom: Double) = setTo(left, top, right - left, bottom - top)
	fun setBounds(left: Int, top: Int, right: Int, bottom: Int) = setTo(left, top, right - left, bottom - top)

	operator fun contains(that: Rectangle) = isContainedIn(that, this)

	infix fun intersects(that: Rectangle): Boolean = intersectsX(that) && intersectsY(that)

	infix fun intersectsX(that: Rectangle): Boolean = that.left <= this.right && that.right >= this.left
	infix fun intersectsY(that: Rectangle): Boolean = that.top <= this.bottom && that.bottom >= this.top

	fun setToIntersection(a: Rectangle, b: Rectangle) = this.apply { a.intersection(b, this) }

	infix fun intersection(that: Rectangle) = intersection(that, Rectangle())

	fun intersection(that: Rectangle, target: Rectangle = Rectangle()) = if (this intersects that) target.setBounds(
			Math.max(this.left, that.left), Math.max(this.top, that.top),
			Math.min(this.right, that.right), Math.min(this.bottom, that.bottom)
	) else null

	fun inflate(dx: Double, dy: Double) {
		x -= dx; width += 2 * dx
		y -= dy; height += 2 * dy
	}

	fun clone() = Rectangle(x, y, width, height)

	override fun toString(): String = "Rectangle($x, $y, $width, $height)"

	companion object {
		fun fromBounds(left: Double, top: Double, right: Double, bottom: Double): Rectangle = Rectangle().setBounds(left, top, right, bottom)
		fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): Rectangle = Rectangle().setBounds(left, top, right, bottom)
		fun isContainedIn(a: Rectangle, b: Rectangle): Boolean = a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height
	}
}