package com.soywiz.korim.geom

data class ISize(var width: Int = 0, var height: Int = width) {
	operator fun contains(v: ISize): Boolean = (v.width <= width) && (v.height <= height)

	operator fun times(v: Double) = ISize((width * v).toInt(), (height * v).toInt())

	fun setTo(width: Int, height: Int) = this.apply {
		this.width = width
		this.height = height
	}

	fun setTo(that: ISize) = setTo(that.width, that.height)

	fun applyScaleMode(container: ISize, mode: ScaleMode): ISize {
		return mode(this, container)
	}

	fun fitTo(container: ISize): ISize = applyScaleMode(container, ScaleMode.SHOW_ALL)

	fun scale(sx: Double, sy: Double = sx) = setTo((this.width * sx).toInt(), (this.height * sy).toInt())

	fun anchoredIn(container: IRectangle, anchor: Anchor): IRectangle {
		return IRectangle(((container.width - this.width) * anchor.sx).toInt(), ((container.height - this.height) * anchor.sy).toInt(), width, height)
	}

	fun getAnchorPosition(anchor: Anchor): IPosition = IPosition((width * anchor.sx).toInt(), (height * anchor.sy).toInt())
}