package com.soywiz.korim.geom

data class IRectangle(val position: IPosition, val size: ISize) {
	constructor(x: Int = 0, y: Int = 0, width: Int = 0, height: Int = 0) : this(IPosition(x, y), ISize(width, height))

	var x: Int set(value) = run { position.x = value }; get() = position.x
	var y: Int set(value) = run { position.y = value }; get() = position.y

	var width: Int set(value) = run { size.width = value }; get() = size.width
	var height: Int set(value) = run { size.height = value }; get() = size.height

	var left: Int set(value) = run { x = value }; get() = x
	var top: Int set(value) = run { y = value }; get() = y

	var right: Int set(value) = run { width = value - x }; get() = x + width
	var bottom: Int set(value) = run { height = value - y }; get() = y + height

	fun setTo(that: IRectangle) = setTo(that.x, that.y, that.width, that.height)

	fun setTo(x: Int, y: Int, width: Int, height: Int) = this.apply {
		this.x = x
		this.y = y
		this.width = width
		this.height = height
	}

	fun setPosition(x: Int, y: Int) = this.apply { this.position.setTo(x, y) }

	fun setSize(width: Int, height: Int) = this.apply {
		this.size.setTo(width, height)
		this.width = width
		this.height = height
	}

	fun setBoundsTo(left: Int, top: Int, right: Int, bottom: Int) = setTo(left, top, right - left, bottom - top)

	fun anchoredIn(container: IRectangle, anchor: Anchor, out: IRectangle = IRectangle()): IRectangle = out.setTo(
		((container.width - this.width) * anchor.sx).toInt(), ((container.height - this.height) * anchor.sy).toInt(),
		width, height
	)

	fun getAnchorPosition(anchor: Anchor, out: IPosition = IPosition()): IPosition = out.setTo((x + width * anchor.sx).toInt(), (y + height * anchor.sy).toInt())

	operator fun contains(v: ISize): Boolean = (v.width <= width) && (v.height <= height)
}
