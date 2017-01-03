package com.soywiz.korim.geom

data class ISize(var width: Int, var height: Int) {
	operator fun contains(v: ISize): Boolean = (v.width <= width) && (v.height <= height)

	operator fun times(v: Double) = ISize((width * v).toInt(), (height * v).toInt())

	fun fitTo(container: ISize): ISize {
		val ratio1 = container.width.toDouble() / width.toDouble()
		val ratio2 = container.height.toDouble() / height.toDouble()
		val size1 = this * ratio1
		val size2 = this * ratio2
		val size = if (size1 in container) size1 else size2
		return size
	}

	fun anchoredIn(container: IRectangle, anchor: Anchor): IRectangle {
		return IRectangle(((container.width - this.width) * anchor.sx).toInt(), ((container.height - this.height) * anchor.sy).toInt(), width, height)
	}

	fun getAnchorPosition(anchor: Anchor): IPosition = IPosition((width * anchor.sx).toInt(), (height * anchor.sy).toInt())
}