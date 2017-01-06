package com.soywiz.korim.geom

class ScaleMode(private val f: (item: ISize, container: ISize, target: ISize) -> Unit) {
	operator fun invoke(item: ISize, container: ISize, target: ISize = ISize()): ISize = target.apply {
		f(item, container, target)
	}

	companion object {
		val COVER = ScaleMode { item, container, target ->
			val s0 = container.width.toDouble() / item.width.toDouble()
			val s1 = container.height.toDouble() / item.height.toDouble()
			target.setTo(item).setToScaled(Math.max(s0, s1))
		}

		val SHOW_ALL = ScaleMode { item, container, target ->
			val s0 = container.width.toDouble() / item.width.toDouble()
			val s1 = container.height.toDouble() / item.height.toDouble()
			target.setTo(item).setToScaled(Math.min(s0, s1))
		}

		val EXACT = ScaleMode { item, container, target ->
			target.setTo(container)
		}

		val NO_SCALE = ScaleMode { item, container, target ->
			target.setTo(item)
		}
	}
}