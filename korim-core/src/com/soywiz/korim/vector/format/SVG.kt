package com.soywiz.korim.vector.format

import com.soywiz.korim.color.NamedColors
import com.soywiz.korim.geom.Vector2
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
import com.soywiz.korio.serialization.xml.Xml
import com.soywiz.korio.util.StrReader
import org.intellij.lang.annotations.Language

class SVG(val root: Xml) : Context2d.Drawable {
	constructor(@Language("xml") str: String) : this(Xml(str))

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

	fun drawElement(xml: Xml, c: Context2d) {
		c.keepApply {
			if (xml.hasAttribute("stroke-width")) {
				lineWidth = xml.double("stroke-width", 1.0)
			}
			if (xml.hasAttribute("stroke")) {
				val strokeStr = xml.str("stroke")
				strokeStyle = when (strokeStr) {
					"none" -> Context2d.None
					else -> Context2d.Color(NamedColors[strokeStr])
				}
			}
			if (xml.hasAttribute("fill")) {
				val fillStr = xml.str("fill")
				fillStyle = when (fillStr) {
					"none" -> Context2d.None
					else -> Context2d.Color(NamedColors[fillStr])
				}
			}

			when (xml.name) {
				"_text_" -> Unit
				"svg" -> drawChildren(xml, c)
				"rect" -> {
					rect(xml.double("x"), xml.double("y"), xml.double("width"), xml.double("height"))
				}
				"circle" -> {
					circle(xml.double("cx"), xml.double("cy"), xml.double("r"))
				}
				"polyline" -> {
					beginPath()
					val points = xml.str("points")
					// @TODO: intelliJ bug: when using Point2d (alias), it removes the import because don't detect it
					val pps = points.split(' ').map { val (x, y) = it.split(',').map { it.toDouble() }; Vector2(x, y) }
					for ((index, p) in pps.withIndex()) {
						if (index == 0) {
							moveTo(p)
						} else {
							lineTo(p)
						}
					}
				}
				"line" -> {
					beginPath()
					moveTo(xml.double("x1"), xml.double("y1"))
					lineTo(xml.double("x2"), xml.double("y2"))
				}
				"g" -> {
					drawChildren(xml, c)
				}
				"path" -> {
					val d = xml.str("d")
					val dr = StrReader(d)
					fun StrReader.readNumber() = skipSpaces().readWhile { !it.isWhitespace() }.toDouble()
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
					beginPath()
					c.path(path)
				}
			}
			c.fillStroke()
		}
	}
}