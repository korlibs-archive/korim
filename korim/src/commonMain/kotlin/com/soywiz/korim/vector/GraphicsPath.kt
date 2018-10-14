package com.soywiz.korim.vector

import com.soywiz.kds.*
import com.soywiz.korma.geom.*

class GraphicsPath(
	commands: IntArrayList = IntArrayList(),
	data: DoubleArrayList = DoubleArrayList(),
	winding: VectorPath.Winding = VectorPath.Winding.EVEN_ODD
) : VectorPath(commands, data, winding), Context2d.SizedDrawable {

	override val width: Int get() = this.getBounds().width.toInt()
	override val height: Int get() = this.getBounds().height.toInt()

	override fun draw(c: Context2d) {
		c.state.path.write(this)
	}

	override fun clone() = GraphicsPath(IntArrayList(commands), DoubleArrayList(data), winding)
}

