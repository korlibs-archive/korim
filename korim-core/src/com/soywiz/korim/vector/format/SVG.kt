package com.soywiz.korim.vector.format

import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.NamedColors
import com.soywiz.korim.geom.Point2d
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.serialization.xml.Xml
import org.intellij.lang.annotations.Language

object SVG {
	fun parseRoot(xml: Xml): Context2d.Drawable {
		if (xml.name != "svg") throw IllegalArgumentException("Expected svg tag")
		val drawables = xml.allChildren.map { parseElement(it) }.filterNotNull()
		return Context2d.FuncDrawable {
			for (d in drawables) d.draw(this)
		}
	}

	fun Context2d.render(xml: Xml) {
		val fill = xml.str("fill", "none")
		val stroke = xml.str("stroke", "none")
		if (fill != "none") {
			fillStyle = Context2d.Color(NamedColors[xml.str("fill")])
			fill()
		}
		if (stroke != "none") {
			lineWidth = xml.double("stroke-width", 1.0)
			strokeStyle = Context2d.Color(NamedColors[xml.str("stroke")])
			stroke()
		}

	}

	fun parseElement(xml: Xml): Context2d.Drawable? {
		if (xml.type != Xml.Type.NODE) return null

		return when (xml.name) {
			"rect" -> {
				Context2d.FuncDrawable {
					rect(xml.double("x"), xml.double("y"), xml.double("width"), xml.double("height"))
					render(xml)
				}
			}
			"circle" -> {
				Context2d.FuncDrawable {
					circle(xml.double("cx"), xml.double("cy"), xml.double("r"))
					render(xml)
				}
			}
			"polyline" -> {
				Context2d.FuncDrawable {
					beginPath()
					val points = xml.str("points")
					val pps = points.split(' ').map { val (x, y) = it.split(',').map { it.toDouble() }; Point2d(x, y) }
					for ((index, p) in pps.withIndex()) {
						if (index == 0) {
							moveTo(p)
						} else {
							lineTo(p)
						}
					}
					render(xml)
				}
			}
			"line" -> {
				Context2d.FuncDrawable {
					beginPath()
					moveTo(xml.double("x1"), xml.double("y1"))
					lineTo(xml.double("x2"), xml.double("y2"))
					render(xml)
				}
			}
			else -> TODO(xml.name)
		}
	}

	operator fun invoke(@Language("xml") str: String) = parseRoot(Xml(str.trim()))
	operator fun invoke(xml: Xml) = parseRoot(xml)
}