package com.soywiz.korim.vector

import com.soywiz.korio.serialization.xml.Xml
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.VectorPath

class SvgBuilder {
	fun toXml(): Xml {
		TODO()
	}
}

fun VectorPath.toSvgPathString(): String {
	val parts = arrayListOf<String>()
	this.visitCmds(
		moveTo = { x, y -> parts += "M$x $y" },
		lineTo = { x, y -> parts += "L$x $y" },
		quadTo = { x1, y1, x2, y2 -> parts += "Q$x1 $y1, $x2 $y2" },
		cubicTo = { x1, y1, x2, y2, x3, y3 -> parts += "C$x1 $y1, $x2 $y2, $x3 $y3" },
		close = { parts += "Z" }
	)
	return parts.joinToString("")
}

interface Shape : Context2d.Drawable {
	fun addBounds(bb: BoundsBuilder): Unit
	fun buildSvg(svg: SvgBuilder): Unit {
	}
}

fun Shape.toSvg(): Xml {
	return SvgBuilder().apply { buildSvg(this) }.toXml()
}

/*
fun GraphicsPath.draw(ctx: Context2d): Unit {
	this.visitCmds(
		moveTo = { x, y -> ctx.moveTo(x, y) },
		lineTo = { x, y -> ctx.lineTo(x, y) },
		quadTo = { x1, y1, x2, y2 -> ctx.quadraticCurveTo(x1, y1, x2, y2) },
		cubicTo = { x1, y1, x2, y2, x3, y3 -> ctx.bezierCurveTo(x1, y1, x2, y2, x3, y3) },
		close = { ctx.closePath() }
	)
}
*/

interface StyledShape : Shape {
	val path: GraphicsPath
	val clip: GraphicsPath?
	val paint: Context2d.Paint
	val transform: Matrix2d

	override fun addBounds(bb: BoundsBuilder): Unit {
		path.addBounds(bb)
	}

	override fun buildSvg(svg: SvgBuilder) {
		super.buildSvg(svg)
	}

	override fun draw(c: Context2d) {
		c.keepTransform {
			c.transform(transform)
			c.beginPath()
			path.draw(c)
			if (clip != null) {
				clip!!.draw(c)
				c.clip()
			}
			drawInternal(c)
		}
	}

	fun drawInternal(c: Context2d) {
	}
}

data class FillShape(
	override val path: GraphicsPath,
	override val clip: GraphicsPath?,
	override val paint: Context2d.Paint,
	override val transform: Matrix2d
) : StyledShape {
	override fun drawInternal(c: Context2d) {
		c.fill(paint)
	}
}

data class PolylineShape(
	override val path: GraphicsPath,
	override val clip: GraphicsPath?,
	override val paint: Context2d.Paint,
	override val transform: Matrix2d,
	val thickness: Double,
	val pixelHinting: Boolean,
	val scaleMode: String,
	val startCaps: Context2d.LineCap,
	val endCaps: Context2d.LineCap,
	val joints: String?,
	val miterLimit: Double
) : StyledShape {
	override fun drawInternal(c: Context2d) {
		c.lineWidth = thickness
		c.lineCap = endCaps
		c.stroke(paint)
	}
}

class CompoundShape(
	val components: List<Shape>
) : Shape {
	override fun addBounds(bb: BoundsBuilder) {
		for (component in components) component.addBounds(bb)
	}

	override fun draw(c: Context2d) {
		for (component in components) component.draw(c)
	}
}