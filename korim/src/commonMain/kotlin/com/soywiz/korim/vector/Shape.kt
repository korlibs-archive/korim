package com.soywiz.korim.vector

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.util.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*

/*
<svg width="80px" height="30px" viewBox="0 0 80 30"
     xmlns="http://www.w3.org/2000/svg">

  <defs>
    <linearGradient id="Gradient01">
      <stop offset="20%" stop-color="#39F" />
      <stop offset="90%" stop-color="#F3F" />
    </linearGradient>
  </defs>

  <rect x="10" y="10" width="60" height="10"
        fill="url(#Gradient01)" />
</svg>
 */

class SvgBuilder(val bounds: Rectangle, val scale: Double) {
	val defs = arrayListOf<Xml>()
	val nodes = arrayListOf<Xml>()

	//val tx = -bounds.x
	//val ty = -bounds.y

	fun toXml(): Xml {
		return Xml.Tag(
			"svg",
			linkedMapOf(
				"width" to "${(bounds.width * scale).niceStr}px",
				"height" to "${(bounds.height * scale).niceStr}px",
				"viewBox" to "0 0 ${(bounds.width * scale).niceStr} ${(bounds.height * scale).niceStr}",
				"xmlns" to "http://www.w3.org/2000/svg",
				"xmlns:xlink" to "http://www.w3.org/1999/xlink"
			),
			listOf(
				Xml.Tag("defs", mapOf(), defs),
				Xml.Tag(
					"g",
					mapOf("transform" to Matrix2d().translate(-bounds.x, -bounds.y).scale(scale, scale).toSvg()),
					nodes
				)
			) //+ nodes
		)
	}
}

private fun Matrix2d.toSvg() = this.run {
	when (getType()) {
		Matrix2d.Type.IDENTITY -> "translate()"
		Matrix2d.Type.TRANSLATE -> "translate(${tx.niceStr}, ${ty.niceStr})"
		Matrix2d.Type.SCALE -> "scale(${a.niceStr}, ${d.niceStr})"
		Matrix2d.Type.SCALE_TRANSLATE -> "translate(${tx.niceStr}, ${ty.niceStr}) scale(${a.niceStr}, ${d.niceStr})"
		else -> "matrix(${a.niceStr}, ${b.niceStr}, ${c.niceStr}, ${d.niceStr}, ${tx.niceStr}, ${ty.niceStr})"
	}
}

fun VectorPath.toSvgPathString(separator: String = " ", decimalPlaces: Int = 1): String {
	val parts = arrayListOf<String>()

	fun Double.fixX() = this.toString(decimalPlaces, skipTrailingZeros = true)
	fun Double.fixY() = this.toString(decimalPlaces, skipTrailingZeros = true)

	this.visitCmds(
		moveTo = { x, y -> parts += "M${x.fixX()} ${y.fixY()}" },
		lineTo = { x, y -> parts += "L${x.fixX()} ${y.fixY()}" },
		quadTo = { x1, y1, x2, y2 -> parts += "Q${x1.fixX()} ${y1.fixY()}, ${x2.fixX()} ${y2.fixY()}" },
		cubicTo = { x1, y1, x2, y2, x3, y3 -> parts += "C${x1.fixX()} ${y1.fixY()}, ${x2.fixX()} ${y2.fixY()}, ${x3.fixX()} ${y3.fixY()}" },
		close = { parts += "Z" }
	)
	return parts.joinToString("")
}

//fun VectorPath.toSvgPathString(scale: Double, tx: Double, ty: Double): String {
//	val parts = arrayListOf<String>()
//
//	//fun Double.fix() = (this * scale).toInt()
//	fun Double.fixX() = ((this + tx) * scale).niceStr
//	fun Double.fixY() = ((this + ty) * scale).niceStr
//
//	this.visitCmds(
//		moveTo = { x, y -> parts += "M${x.fixX()} ${y.fixY()}" },
//		lineTo = { x, y -> parts += "L${x.fixX()} ${y.fixY()}" },
//		quadTo = { x1, y1, x2, y2 -> parts += "Q${x1.fixX()} ${y1.fixY()}, ${x2.fixX()} ${y2.fixY()}" },
//		cubicTo = { x1, y1, x2, y2, x3, y3 -> parts += "C${x1.fixX()} ${y1.fixY()}, ${x2.fixX()} ${y2.fixY()}, ${x3.fixX()} ${y3.fixY()}" },
//		close = { parts += "Z" }
//	)
//	return parts.joinToString("")
//}

interface Shape : Context2d.Drawable {
	fun addBounds(bb: BoundsBuilder): Unit
	fun buildSvg(svg: SvgBuilder): Unit {
	}

	fun containsPoint(x: Double, y: Double): Boolean
}

fun Shape.getBounds(out: Rectangle = Rectangle()) = out.apply {
	val bb = BoundsBuilder()
	addBounds(bb)
	bb.getBounds(out)
}

fun Shape.toSvg(scale: Double = 1.0): Xml = SvgBuilder(this.getBounds(), scale).apply { buildSvg(this) }.toXml()

interface StyledShape : Shape {
	val path: GraphicsPath
	val clip: GraphicsPath?
	val paint: Context2d.Paint
	val transform: Matrix2d

	override fun addBounds(bb: BoundsBuilder): Unit {
		path.addBounds(bb)
	}

	override fun buildSvg(svg: SvgBuilder) {
		svg.nodes += Xml.Tag(
			"path", mapOf(
				//"d" to path.toSvgPathString(svg.scale, svg.tx, svg.ty)
				"d" to path.toSvgPathString()
			) + getSvgXmlAttributes(svg), listOf()
		)
	}

	fun getSvgXmlAttributes(svg: SvgBuilder): Map<String, String> = mapOf(
		//"transform" to transform.toSvg()
	)

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

private fun colorToSvg(color: RGBA): String {
	val r = color.r
	val g = color.g
	val b = color.b
	val af = color.af
	return "rgba($r,$g,$b,$af)"
}

fun Context2d.Paint.toSvg(svg: SvgBuilder): String {
	val id = svg.defs.size
	/*
	svg.defs += when (this) {
		is Context2d.Paint.
		Xml.Tag("")
	}
	return "url(#def$id)"
	*/
	when (this) {
		is Context2d.Gradient -> {
			val stops = (0 until numberOfStops).map {
				val ratio = this.stops[it]
				val color = RGBA(this.colors[it])
				Xml.Tag("stop", mapOf("offset" to "${ratio * 100}%", "stop-color" to colorToSvg(color)), listOf())
			}

			when (this) {
				is Context2d.Gradient -> {
					when (this.kind) {
						Context2d.Gradient.Kind.LINEAR -> {
							svg.defs += Xml.Tag(
								"linearGradient",
								mapOf(
									"id" to "def$id",
									"x1" to "$x0", "y1" to "$y0",
									"x2" to "$x1", "y2" to "$y1",
									"gradientTransform" to transform.toSvg()
								),
								stops
							)
						}
						Context2d.Gradient.Kind.RADIAL -> {
							svg.defs += Xml.Tag(
								"radialGradient",
								mapOf(
									"id" to "def$id",
									"cx" to "$x0", "cy" to "$y0",
									"fx" to "$x1", "fy" to "$y1",
									"r" to "$r1",
									"gradientTransform" to transform.toSvg()
								),
								stops
							)
						}
					}
				}
			}
			return "url(#def$id)"
		}
		is Context2d.BitmapPaint -> {
			//<pattern id="img1" patternUnits="userSpaceOnUse" width="100" height="100">
			//<image xlink:href="wall.jpg" x="0" y="0" width="100" height="100" />
			//</pattern>


			svg.defs += Xml.Tag(
				"pattern", mapOf(
					"id" to "def$id",
					"patternUnits" to "userSpaceOnUse",
					"width" to "${bitmap.width}",
					"height" to "${bitmap.height}",
					"patternTransform" to transform.toSvg()
				), listOf(
					Xml.Tag(
						"image",
						mapOf(
							"xlink:href" to bitmap.toUri(),
							"width" to "${bitmap.width}",
							"height" to "${bitmap.height}"
						),
						listOf<Xml>()
					)
				)
			)
			return "url(#def$id)"
		}
		is Context2d.Color -> {
			return colorToSvg(color)
		}
		else -> return "red"
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

	override fun getSvgXmlAttributes(svg: SvgBuilder) = super.getSvgXmlAttributes(svg) + mapOf(
		"fill" to paint.toSvg(svg)
	)

	override fun containsPoint(x: Double, y: Double): Boolean {
		val tx = transform.transformX(x, y)
		val ty = transform.transformY(x, y)
		if (clip != null) return clip.containsPoint(tx, ty)
		return path.containsPoint(tx, ty)
	}
}

data class PolylineShape(
	override val path: GraphicsPath,
	override val clip: GraphicsPath?,
	override val paint: Context2d.Paint,
	override val transform: Matrix2d,
	val thickness: Double,
	val pixelHinting: Boolean,
	val scaleMode: Context2d.ScaleMode,
	val startCaps: Context2d.LineCap,
	val endCaps: Context2d.LineCap,
	val joints: String?,
	val miterLimit: Double
) : StyledShape {
	override fun drawInternal(c: Context2d) {
		c.lineScaleMode = scaleMode
		c.lineWidth = thickness
		c.lineCap = endCaps
		c.stroke(paint)
	}

	override fun containsPoint(x: Double, y: Double): Boolean {
		val tx = transform.transformX(x, y)
		val ty = transform.transformY(x, y)
		if (clip != null) return clip.containsPoint(tx, ty)
		return path.containsPoint(tx, ty)
	}

	override fun getSvgXmlAttributes(svg: SvgBuilder) = super.getSvgXmlAttributes(svg) + mapOf(
		"stroke-width" to "$thickness",
		"stroke" to paint.toSvg(svg)
	)
}

class CompoundShape(
	val components: List<Shape>
) : Shape {
	override fun addBounds(bb: BoundsBuilder) = run { for (component in components) component.addBounds(bb) }
	override fun draw(c: Context2d) = run { for (component in components) component.draw(c) }
	override fun buildSvg(svg: SvgBuilder) = run { for (component in components) component.buildSvg(svg) }
	override fun containsPoint(x: Double, y: Double): Boolean {
		return components.any { it.containsPoint(x, y) }
	}
}
