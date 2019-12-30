package com.soywiz.korim.vector

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*

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
					mapOf("transform" to Matrix().translate(-bounds.x, -bounds.y).scale(scale, scale).toSvg()),
					nodes
				)
			) //+ nodes
		)
	}
}

private fun Matrix.toSvg() = this.run {
	when (getType()) {
		Matrix.Type.IDENTITY -> "translate()"
		Matrix.Type.TRANSLATE -> "translate(${tx.niceStr}, ${ty.niceStr})"
		Matrix.Type.SCALE -> "scale(${a.niceStr}, ${d.niceStr})"
		Matrix.Type.SCALE_TRANSLATE -> "translate(${tx.niceStr}, ${ty.niceStr}) scale(${a.niceStr}, ${d.niceStr})"
		else -> "matrix(${a.niceStr}, ${b.niceStr}, ${c.niceStr}, ${d.niceStr}, ${tx.niceStr}, ${ty.niceStr})"
	}
}

fun VectorPath.toSvgPathString(separator: String = " ", decimalPlaces: Int = 1): String {
	val parts = arrayListOf<String>()

	fun Double.fixX() = this.toStringDecimal(decimalPlaces, skipTrailingZeros = true)
	fun Double.fixY() = this.toStringDecimal(decimalPlaces, skipTrailingZeros = true)

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

interface Shape : Context2d.BoundsDrawable {
	fun addBounds(bb: BoundsBuilder): Unit
	fun buildSvg(svg: SvgBuilder): Unit = Unit

    // Unoptimized version
    override val bounds: Rectangle get() = BoundsBuilder().also { addBounds(it) }.getBounds()
	fun containsPoint(x: Double, y: Double): Boolean = bounds.contains(x, y)
}

fun Shape.getBounds(out: Rectangle = Rectangle()) = out.apply {
	val bb = BoundsBuilder()
	addBounds(bb)
	bb.getBounds(out)
}

fun Shape.toSvg(scale: Double = 1.0): Xml = SvgBuilder(this.getBounds(), scale).apply { buildSvg(this) }.toXml()
fun Context2d.Drawable.toShape(width: Int, height: Int): Shape = buildShape(width, height) { draw(this@toShape) }
fun Context2d.Drawable.toSvg(width: Int, height: Int, scale: Double = 1.0): Xml = toShape(width, height).toSvg(scale)

fun Context2d.SizedDrawable.toShape(): Shape = toShape(width, height)
fun Context2d.SizedDrawable.toSvg(scale: Double = 1.0): Xml = toSvg(width, height, scale)

interface StyledShape : Shape {
	val path: GraphicsPath? get() = null
	val clip: GraphicsPath?
	val paint: Context2d.Paint
	val transform: Matrix

	override fun addBounds(bb: BoundsBuilder): Unit {
        path?.let { bb.add(it) }
	}

	override fun buildSvg(svg: SvgBuilder) {
		svg.nodes += Xml.Tag(
			"path", mapOf(
				//"d" to path.toSvgPathString(svg.scale, svg.tx, svg.ty)
				"d" to (path?.toSvgPathString() ?: ""),
                "transform" to transform.toSvg()
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
			path?.draw(c)
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
	override val transform: Matrix
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
	override val transform: Matrix,
	val thickness: Double,
	val pixelHinting: Boolean,
	val scaleMode: Context2d.ScaleMode,
	val startCaps: Context2d.LineCap,
	val endCaps: Context2d.LineCap,
	val lineJoin: Context2d.LineJoin,
	val miterLimit: Double
) : StyledShape {
    @Suppress("unused")
    @Deprecated("Use lineJoin instead", replaceWith = ReplaceWith("lineJoin.name"))
    val joints: String? = lineJoin.name

    @Deprecated("Use constructor with lineJoin: Context2d.LineJoin")
    constructor(
        path: GraphicsPath,
        clip: GraphicsPath?,
        paint: Context2d.Paint,
        transform: Matrix,
        thickness: Double,
        pixelHinting: Boolean,
        scaleMode: Context2d.ScaleMode,
        startCaps: Context2d.LineCap,
        endCaps: Context2d.LineCap,
        joints: String?,
        miterLimit: Double
    ) : this(path, clip, paint, transform, thickness, pixelHinting, scaleMode, startCaps, endCaps, when (joints) {
        null -> Context2d.LineJoin.MITER
        "MITER", "miter" -> Context2d.LineJoin.MITER
        "BEVEL", "bevel" -> Context2d.LineJoin.BEVEL
        "ROUND", "round" -> Context2d.LineJoin.ROUND
        else -> Context2d.LineJoin.MITER
    }, miterLimit)

    private val tempBB = BoundsBuilder()
    private val tempRect = Rectangle()

    override fun addBounds(bb: BoundsBuilder): Unit {
        tempBB.reset()
        tempBB.add(path)
        tempBB.getBounds(tempRect)

        val halfThickness = thickness / 2
        tempRect.inflate(max(halfThickness, 0.0), max(halfThickness, 0.0))

        //println("PolylineShape.addBounds: thickness=$thickness, rect=$tempRect")
        bb.add(tempRect)
    }

    override fun drawInternal(c: Context2d) {
		c.lineScaleMode = scaleMode
		c.lineWidth = thickness
		c.lineCap = endCaps
        c.lineJoin = lineJoin
		c.stroke(paint)
	}

	override fun containsPoint(x: Double, y: Double): Boolean {
		val tx = transform.transformX(x, y)
		val ty = transform.transformY(x, y)
		if (clip != null) return clip.containsPoint(tx, ty)
		return path.containsPoint(tx, ty)
	}

	override fun getSvgXmlAttributes(svg: SvgBuilder) = super.getSvgXmlAttributes(svg) + mapOf(
        "fill" to "none",
		"stroke-width" to "$thickness",
		"stroke" to paint.toSvg(svg)
	)
}

class CompoundShape(
	val components: List<Shape>
) : Shape {
	override fun addBounds(bb: BoundsBuilder) = run { components.fastForEach { it.addBounds(bb)}  }
	override fun draw(c: Context2d) = c.buffering { components.fastForEach { it.draw(c) } }
	override fun buildSvg(svg: SvgBuilder) = run { components.fastForEach { it.buildSvg(svg) } }
	override fun containsPoint(x: Double, y: Double): Boolean {
		return components.any { it.containsPoint(x, y) }
	}
}

class TextShape(
    val text: String,
    val x: Double,
    val y: Double,
    val font: Context2d.Font,
    override val clip: GraphicsPath?,
    val fill: Context2d.Paint?,
    val stroke: Context2d.Paint?,
    val halign: Context2d.HorizontalAlign = Context2d.HorizontalAlign.LEFT,
    val valign: Context2d.VerticalAlign = Context2d.VerticalAlign.TOP,
    override val transform: Matrix = Matrix()
) : StyledShape {
    override val paint: Context2d.Paint get() = fill ?: stroke ?: Context2d.None

    override fun addBounds(bb: BoundsBuilder) {
        bb.add(x, y)
        bb.add(x + font.size * text.length, y + font.size) // @TODO: this is not right since we don't have information about Glyph metrics
    }
    override fun drawInternal(c: Context2d) {
        c.font(font, halign, valign) {
            if (fill != null) c.fillText(text, x, y)
            if (stroke != null) c.strokeText(text, x, y)
        }
    }
    override fun buildSvg(svg: SvgBuilder) {
        svg.nodes += Xml.Tag(
            "text", mapOf(
                "x" to x,
                "y" to y,
                "fill" to (fill?.toSvg(svg) ?: "none"),
                "stroke" to (stroke?.toSvg(svg) ?: "none"),
                "font-family" to font.name,
                "font-size" to "${font.size}px",
                "text-anchor" to when (halign) {
                    Context2d.HorizontalAlign.LEFT -> "start"
                    Context2d.HorizontalAlign.CENTER -> "middle"
                    Context2d.HorizontalAlign.RIGHT -> "end"
                },
                "alignment-baseline" to when (valign) {
                    Context2d.VerticalAlign.TOP -> "hanging"
                    Context2d.VerticalAlign.MIDDLE -> "middle"
                    Context2d.VerticalAlign.BASELINE -> "baseline"
                    Context2d.VerticalAlign.BOTTOM -> "bottom"
                },
                "transform" to transform.toSvg()
            ), listOf(
                Xml.Text(text)
            )
        )
    }
}
