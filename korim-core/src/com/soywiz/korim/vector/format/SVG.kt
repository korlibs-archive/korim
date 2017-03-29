package com.soywiz.korim.vector.format

import com.soywiz.korim.color.NamedColors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
import com.soywiz.korio.serialization.xml.Xml
import com.soywiz.korio.serialization.xml.allChildren
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.math.Matrix2d
import org.intellij.lang.annotations.Language

class SVG(val root: Xml) : Context2d.SizedDrawable {
	constructor(@Language("xml") str: String) : this(Xml(str))

	override val width = root.int("width", 128)
	override val height = root.int("height", 128)

	class Style {
		val props = hashMapOf<String, Any?>()
	}

	enum class GradientUnits {
		USER_SPACE_ON_USER,
		OBJECT_BOUNDING_BOX,
	}

	val defs = hashMapOf<String, Context2d.Paint>()

	//interface Def

	fun parsePercent(str: String): Double {
		return if (str.endsWith("%")) {
			str.substr(0, -1).toDouble() / 100.0
		} else {
			str.toDouble()
		}
	}

	fun parseStops(xml: Xml): List<Pair<Double, Int>> {
		val out = arrayListOf<Pair<Double, Int>>()
		for (stop in xml.children("stop")) {
			val offset = parsePercent(stop.str("offset"))
			val colorStop = NamedColors[stop.str("stop-color")]
			val alphaStop = stop.double("stop-opacity", 1.0)
			out += Pair(offset, RGBA.packRGB_A(colorStop, (alphaStop * 255).toInt()))
		}
		return out
	}

	fun parseDef(def: Xml) {
		val type = def.nameLC
		when (type) {
			"lineargradient", "radialgradient" -> {
				val id = def.str("id").toLowerCase()
				val x0 = def.double("x1", 0.0)
				val y0 = def.double("y1", 0.0)
				val x1 = def.double("x2", 1.0)
				val y1 = def.double("y2", 1.0)
				val stops = parseStops(def)
				val g: Context2d.Gradient = if (type == "lineargradient") {
					//println("Linear: ($x0,$y0)-($x1-$y1)")
					Context2d.LinearGradient(x0, y0, x1, y1)
				} else {
					val r0 = def.double("r0", 0.0)
					val r1 = def.double("r1", 0.0)
					Context2d.RadialGradient(x0, y0, r0, x1, y1, r1)
				}
				for ((offset, color) in stops) {
					//println(" - $offset: $color")
					g.addColorStop(offset, color)
				}
				//println("Gradient: $g")
				defs[id] = g
			}
			"style" -> {
			}
			"_text_" -> {
			}
			else -> {
				println("Unhandled def: '$type'")
			}
		}
	}

	fun parseDefs() {
		for (def in root["defs"].allChildren) parseDef(def)
	}

	init {
		parseDefs()
	}

	override fun draw(c: Context2d) {
		c.keep {
			c.strokeStyle = Context2d.None
			c.fillStyle = Context2d.None
			drawElement(root, c)
		}
	}

	fun drawChildren(xml: Xml, c: Context2d) {
		for (child in xml.allChildren) {
			drawElement(child, c)
		}
	}

	fun parseFillStroke(c: Context2d, str2: String, bounds: Rectangle): Context2d.Paint {
		val str = str2.toLowerCase()
		val res = if (str.startsWith("url(")) {
			val urlPattern = str.substr(4, -1)
			if (urlPattern.startsWith("#")) {
				val idName = urlPattern.substr(1).toLowerCase()
				val def = defs[idName]
				if (def == null) {
					println(defs)
					println("Can't find svg definition '$idName'")
				}
				def ?: c.none
			} else {
				println("Unsupported $str")
				c.none
			}
		} else {
			when (str) {
				"none" -> c.none
				else -> c.createColor(NamedColors[str])
			}
		}
		if (res is Context2d.Gradient) {
			val m = Matrix2d()
			m.scale(bounds.width, bounds.height)
			val out = res.applyMatrix(m)
			//println(out)
			return out
		} else {
			return res
		}
	}

	fun drawElement(xml: Xml, c: Context2d) = c.keepApply {
		val bounds = Rectangle()
		val nodeName = xml.nameLC

		when (nodeName) {
			"_text_" -> Unit
			"svg" -> drawChildren(xml, c)
			"lineargradient", "radialgradient" -> {
				parseDef(xml)
			}
			"rect" -> {
				val x = xml.double("x")
				val y = xml.double("y")
				val width = xml.double("width")
				val height = xml.double("height")
				val rx = xml.double("rx")
				val ry = xml.double("ry")
				bounds.setTo(x, y, width, height)
				roundRect(x, y, width, height, rx, ry)
			}
			"circle" -> {
				val cx = xml.double("cx")
				val cy = xml.double("cy")
				val radius = xml.double("r")
				circle(cx, cy, radius)
				bounds.setBounds(cx - radius, cy - radius, cx + radius, cy + radius)
			}
			"polyline", "polygon" -> {
				beginPath()
				val ss = StrReader(xml.str("points"))

				val pps = ListReader(mapWhile(cond = { ss.hasMore }, gen = {
					ss.skipWhile { !it.isNumeric }
					val out = ss.readWhile { it.isNumeric }.toDouble()
					ss.skipWhile { !it.isNumeric }
					out
				}))
				val path = GraphicsPath()
				var edges = 0
				path.moveTo(pps.read(), pps.read())
				while (pps.hasMore) {
					val x = pps.read();
					val y = pps.read()
					path.lineTo(x, y)
					edges++
				}
				if (nodeName == "polygon") path.close()
				path.getBounds(bounds)
				//println("bounds: $bounds, edges: $edges")
				c.path(path)
			}
			"line" -> {
				beginPath()
				val x1 = xml.double("x1")
				val y1 = xml.double("y1")
				val x2 = xml.double("x2")
				val y2 = xml.double("y2")
				moveTo(x1, y1)
				lineTo(x2, y2)
				bounds.setBounds(x1, y1, x2, y2)
			}
			"g" -> {
			}
			"text" -> {
				fillText(xml.text, xml.double("x") + xml.double("dx"), xml.double("y") + xml.double("dy"))
			}
			"path" -> {
				val d = xml.str("d")
				val dr = StrReader(d)
				fun StrReader.readNumber() = skipSpaces().readWhile { it.isDigit() || it == '-' || it == '.' }.toDouble()
				val path = GraphicsPath()
				while (!dr.eof) {
					dr.skipSpaces()
					val cmd = dr.read()
					when (cmd) {
						'M' -> path.moveTo(dr.readNumber(), dr.readNumber())
						'm' -> path.rMoveTo(dr.readNumber(), dr.readNumber())
						'L' -> path.lineTo(dr.readNumber(), dr.readNumber())
						'l' -> path.rLineTo(dr.readNumber(), dr.readNumber())
						'Q' -> path.quadTo(dr.readNumber(), dr.readNumber(), dr.readNumber(), dr.readNumber())
						'q' -> path.rQuadTo(dr.readNumber(), dr.readNumber(), dr.readNumber(), dr.readNumber())
						'C' -> path.cubicTo(dr.readNumber(), dr.readNumber(), dr.readNumber(), dr.readNumber(), dr.readNumber(), dr.readNumber())
						'c' -> path.rCubicTo(dr.readNumber(), dr.readNumber(), dr.readNumber(), dr.readNumber(), dr.readNumber(), dr.readNumber())
						'H' -> path.moveToH(dr.readNumber())
						'h' -> path.rMoveToH(dr.readNumber())
						'V' -> path.moveToV(dr.readNumber())
						'v' -> path.rMoveToV(dr.readNumber())
						'Z' -> path.close()
						'z' -> path.close()
						else -> TODO("Unsupported $cmd")
					}
				}
				path.getBounds(bounds)
				beginPath()
				c.path(path)
			}
		}

		if (xml.hasAttribute("stroke-width")) {
			lineWidth = xml.double("stroke-width", 1.0)
		}
		if (xml.hasAttribute("stroke")) {
			strokeStyle = parseFillStroke(c, xml.str("stroke"), bounds)
		}
		if (xml.hasAttribute("fill")) {
			fillStyle = parseFillStroke(c, xml.str("fill"), bounds)
		}
		if (xml.hasAttribute("font-size")) {
			font = font.copy(size = xml.double("font-size"))
		}
		if (xml.hasAttribute("font-family")) {
			font = font.copy(name = xml.str("font-family"))
		}
		if (xml.hasAttribute("text-anchor")) {
			horizontalAlign = when (xml.str("text-anchor").toLowerCase()) {
				"left" -> Context2d.HorizontalAlign.LEFT
				"center", "middle" -> Context2d.HorizontalAlign.CENTER
				"right" -> Context2d.HorizontalAlign.RIGHT
				else -> horizontalAlign
			}
		}
		if (xml.hasAttribute("fill-opacity")) {
			globalAlpha = xml.double("fill-opacity", 1.0)
		}

		when (nodeName) {
			"g" -> {
				drawChildren(xml, c)
			}
		}

		c.fillStroke()
	}
}