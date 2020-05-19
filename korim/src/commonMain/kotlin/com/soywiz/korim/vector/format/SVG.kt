package com.soywiz.korim.vector.format

import com.soywiz.kds.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.collections.set

class SVG(val root: Xml, val warningProcessor: ((message: String) -> Unit)? = null) : SizedDrawable {
	//constructor(@Language("xml") str: String) : this(Xml(str))
	constructor(str: String) : this(Xml(str))

	val x = root.int("x", 0)
	val y = root.int("y", 0)

	val dwidth = root.double("width", 128.0)
	val dheight = root.double("height", 128.0)
	val viewBox = root.getString("viewBox") ?: "0 0 $dwidth $dheight"
	val viewBoxNumbers = viewBox.split(' ').map { it.trim().toDoubleOrNull() ?: 0.0 }
	val viewBoxRectangle = Rectangle(
		viewBoxNumbers.getOrElse(0) { 0.0 },
		viewBoxNumbers.getOrElse(1) { 0.0 },
		viewBoxNumbers.getOrElse(2) { dwidth },
		viewBoxNumbers.getOrElse(3) { dheight }
	)

	override val width get() = viewBoxRectangle.width.toInt()
	override val height get() = viewBoxRectangle.height.toInt()

	class Style {
		val props = hashMapOf<String, Any?>()
	}

	enum class GradientUnits {
		USER_SPACE_ON_USER,
		OBJECT_BOUNDING_BOX,
	}

	val defs = hashMapOf<String, Paint>()

	//interface Def

	fun parsePercent(str: String): Double {
		return if (str.endsWith("%")) {
			str.substr(0, -1).toDouble() / 100.0
		} else {
			str.toDouble()
		}
	}

	fun parseStops(xml: Xml): List<Pair<Double, RGBA>> {
		val out = arrayListOf<Pair<Double, RGBA>>()
		for (stop in xml.children("stop")) {
			val offset = parsePercent(stop.str("offset"))
			val colorStop = Colors.Default[stop.str("stop-color")]
			val alphaStop = stop.double("stop-opacity", 1.0)
			out += Pair(offset, RGBA(colorStop.rgb, (alphaStop * 255).toInt()))
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
				val href = def.strNull("xlink:href")

				val g: GradientPaint = if (type == "lineargradient") {
					//println("Linear: ($x0,$y0)-($x1-$y1)")
					GradientPaint(GradientKind.LINEAR, x0, y0, 0.0, x1, y1, 0.0)
				} else {
					val r0 = def.double("r0", 0.0)
					val r1 = def.double("r1", 0.0)
					GradientPaint(GradientKind.RADIAL, x0, y0, r0, x1, y1, r1)
				}

				def.strNull("xlink:href")?.let {
					val id = it.trim('#')
					val original = defs[id] as? GradientPaint?
					//println("href: $it --> $original")
					original?.let {
						g.stops.add(original.stops)
						g.colors.add(original.colors)
					}
				}

				for ((offset, color) in stops) {
					//println(" - $offset: $color")
					g.addColorStop(offset, color)
				}
				//println("Gradient: $g")
				def.getString("gradientTransform")?.let {
					g.transform.premultiply(parseTransform(it))
				}

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
		for (def in root["defs"].allChildren.filter { !it.isComment }) parseDef(def)
	}

	init {
		parseDefs()
	}

	override fun draw(c: Context2d) {
		c.keep {
			c.strokeStyle = NonePaint
			c.fillStyle = NonePaint
			drawElement(root, c)
		}
	}

	fun drawChildren(xml: Xml, c: Context2d) {
		for (child in xml.allChildren) {
			drawElement(child, c)
		}
	}

	fun parseFillStroke(c: Context2d, str2: String, bounds: Rectangle): Paint {
		val str = str2.toLowerCase().trim()
		val res = when {
            str.startsWith("url(") -> {
                val urlPattern = str.substr(4, -1)
                if (urlPattern.startsWith("#")) {
                    val idName = urlPattern.substr(1).toLowerCase()
                    val def = defs[idName]
                    if (def == null) {
                        println(defs)
                        println("Can't find svg definition '$idName'")
                    }
                    def ?: NonePaint
                } else {
                    println("Unsupported $str")
                    NonePaint
                }
            }
            str.startsWith("rgba(") -> {
                val components = str.removePrefix("rgba(").removeSuffix(")").split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
                ColorPaint(RGBA(components[0].toInt(), components[1].toInt(), components[2].toInt(), (components[3] * 255).toInt()))
            }
            else -> when (str) {
                "none" -> NonePaint
                else -> c.createColor(Colors.Default[str])
            }
        }
        return when (res) {
            is GradientPaint -> {
                val m = Matrix()
                m.scale(bounds.width, bounds.height)
                val out = res.applyMatrix(m)
                //println(out)
                out
            }
            else -> {
                res
            }
        }
	}

	fun drawElement(xml: Xml, c: Context2d): Context2d = c.keepApply {
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
				}).toList())
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
			}
			"path" -> {
				val d = xml.str("d")
				val tokens = tokenizePath(d)
				val tl = ListReader(tokens)

				fun dumpTokens() = run { for ((n, token) in tokens.withIndex()) warningProcessor?.invoke("- $n: $token") }
				fun isNextNumber(): Boolean = if (tl.hasMore) tl.peek() is PathTokenNumber else false
				fun readNumber(): Double {
					while (tl.hasMore) {
						val token = tl.read()
						if (token is PathTokenNumber) return token.value
                        warningProcessor?.invoke("Invalid path (expected number but found $token) at ${tl.position - 1}")
						dumpTokens()
					}
					return 0.0
				}
                fun n(): Double = readNumber()

				fun readNextTokenCmd(): Char? {
					while (tl.hasMore) {
						val token = tl.read()
						if (token is PathTokenCmd) return token.id
                        warningProcessor?.invoke("Invalid path (expected command but found $token) at ${tl.position - 1}")
						dumpTokens()
					}
					return null
				}

				//dumpTokens()

				beginPath()
				while (tl.hasMore) {
					val cmd = readNextTokenCmd() ?: break
					when (cmd) {
						'M' -> {
							moveTo(n(), n())
							while (isNextNumber()) lineTo(n(), n())
						}
						'm' -> {
							rMoveTo(n(), n())
							while (isNextNumber()) rLineTo(n(), n())
						}
						'L' -> while (isNextNumber()) lineTo(n(), n())
						'l' -> while (isNextNumber()) rLineTo(n(), n())
						'H' -> while (isNextNumber()) lineToH(n())
						'h' -> while (isNextNumber()) rLineToH(n())
						'V' -> while (isNextNumber()) lineToV(n())
						'v' -> while (isNextNumber()) rLineToV(n())
						'Q' -> while (isNextNumber()) quadTo(n(), n(), n(), n())
						'q' -> while (isNextNumber()) rQuadTo(n(), n(), n(), n())
						'C' -> while (isNextNumber()) cubicTo(n(), n(), n(), n(), n(), n())
						'c' -> while (isNextNumber()) rCubicTo(n(), n(), n(), n(), n(), n())

                        // https://developer.mozilla.org/en-US/docs/Web/SVG/Tutorial/Paths
                        // @TODO: Cubic using the last position?
                        'S', 's' -> while (isNextNumber()) {
                            val x2 = n()
                            val y2 = n()
                            val x = n()
                            val y = n()
                            val x1 = x2 // @TODO: Reflected version of x2
                            val y1 = y2 // @TODO: Reflected version of y2
                            if (cmd == 's') {
                                rCubicTo(x1, y1, x2, y2, x, y)
                                //rLineTo(x, y)
                            } else {
                                cubicTo(x1, y1, x2, y2, x, y)
                                //lineTo(x, y)
                            }
                        }

                        'A' -> TODO("arcs not implemented")
                        'a' -> TODO("arcs not implemented")

                        'Z' -> close()
						'z' -> close()
						else -> TODO("Unsupported command '$cmd' : Parsed: '${state.path.toSvgPathString()}', Original: '$d'")
					}
				}
                warningProcessor?.invoke("Parsed SVG Path: '${state.path.toSvgPathString()}'")
                warningProcessor?.invoke("Original SVG Path: '$d'")
                warningProcessor?.invoke("Points: ${state.path.getPoints()}")
				getBounds(bounds)
			}
		}

		if (xml.hasAttribute("stroke-width")) {
			lineWidth = xml.double("stroke-width", 1.0)
		}
		if (xml.hasAttribute("stroke")) {
			strokeStyle = parseFillStroke(c, xml.str("stroke"), bounds)
		}
		if (xml.hasAttribute("fill")) applyFill(c, xml.str("fill"), bounds)
		if (xml.hasAttribute("font-size")) {
            fontSize = parseSizeAsDouble(xml.str("font-size"))
		}
		if (xml.hasAttribute("font-family")) {
			font = fontRegistry[xml.str("font-family")]
		}
		if (xml.hasAttribute("style")) {
			applyStyle(c, SvgStyle.parse(xml.str("style"), warningProcessor), bounds)
		}
		if (xml.hasAttribute("transform")) {
			applyTransform(state, parseTransform(xml.str("transform")))
		}
		if (xml.hasAttribute("text-anchor")) {
			horizontalAlign = when (xml.str("text-anchor").toLowerCase().trim()) {
				"left" -> HorizontalAlign.LEFT
				"center", "middle" -> HorizontalAlign.CENTER
				"right", "end" -> HorizontalAlign.RIGHT
				else -> horizontalAlign
			}
		}
        if (xml.hasAttribute("alignment-baseline")) {
            verticalAlign = when (xml.str("alignment-baseline").toLowerCase().trim()) {
                "hanging" -> VerticalAlign.TOP
                "center", "middle" -> VerticalAlign.MIDDLE
                "baseline" -> VerticalAlign.BASELINE
                "bottom" -> VerticalAlign.BOTTOM
                else -> verticalAlign
            }
        }
		if (xml.hasAttribute("fill-opacity")) {
			globalAlpha = xml.double("fill-opacity", 1.0)
		}

		when (nodeName) {
			"g" -> {
				drawChildren(xml, c)
			}
            "text" -> {
                fillText(xml.text.trim(), xml.double("x") + xml.double("dx"), xml.double("y") + xml.double("dy"))
            }
		}

		c.fillStroke()
	}

    fun parseSizeAsDouble(size: String): Double {
        return size.filter { it !in 'a'..'z' && it !in 'A'..'Z' }.toDoubleOrNull() ?: 16.0
    }

	fun applyFill(c: Context2d, str: String, bounds: Rectangle) {
		c.fillStyle = parseFillStroke(c, str, bounds)
	}

	private fun applyTransform(state: Context2d.State, transform: Matrix) {
		//println("Apply transform $transform to $state")
		state.transform.premultiply(transform)
	}

	private fun applyStyle(c: Context2d, style: SvgStyle, bounds: Rectangle) {
		//println("Apply style $style to $c")
		for ((k, v) in style.styles) {
			//println("$k <-- $v")
			when (k) {
				"fill" -> applyFill(c, v, bounds)
				else -> warningProcessor?.invoke("Unsupported style $k in css")
			}
		}
	}

	fun parseTransform(str: String): Matrix {
		val tokens = SvgStyle.tokenize(str)
		val tr = ListReader(tokens)
		val out = Matrix()
		//println("Not implemented: parseTransform: $str: $tokens")
		while (tr.hasMore) {
			val id = tr.read().toLowerCase()
			val args = arrayListOf<String>()
			if (tr.peek() == "(") {
				tr.read()
				while (true) {
					if (tr.peek() == ")") {
						tr.read()
						break
					}
					if (tr.peek() == ",") {
						tr.read()
						continue
					}
					args += tr.read()
				}
			}
			val doubleArgs = args.map { it.toDoubleOrNull() ?: 0.0 }
			fun double(index: Int) = doubleArgs.getOrElse(index) { 0.0 }
			when (id) {
				"translate" -> out.pretranslate(double(0), double(1))
				"scale" -> out.prescale(double(0), double(1))
				"matrix" -> out.premultiply(double(0), double(1), double(2), double(3), double(4), double(5))
				else -> invalidOp("Unsupported transform $id : $args : $doubleArgs ($str)")
			}
			//println("ID: $id, args=$args")
		}
		return out
	}

	companion object {
		fun tokenizePath(str: String): List<PathToken> {
			val sr = StrReader(str)
			fun StrReader.skipSeparators() {
				skipWhile { it == ',' || it == ' ' || it == '\t' || it == '\n' || it == '\r' }
			}

			fun StrReader.readNumber(): Double {
				skipSeparators()
				var first = true
				val str = readWhile {
					if (first) {
						first = false
						it.isDigit() || it == '-' || it == '+'
					} else {
						it.isDigit() || it == '.'
					}
				}
				return if (str.isEmpty()) 0.0 else try {
					str.toDouble()
				} catch (e: Throwable) {
					e.printStackTrace()
					0.0
				}
			}

			val out = arrayListOf<PathToken>()
			while (sr.hasMore) {
				sr.skipSeparators()
				val c = sr.peekChar()
				out += if (c in '0'..'9' || c == '-' || c == '+') {
					PathTokenNumber(sr.readNumber())
				} else {
					PathTokenCmd(sr.readChar())
				}
			}
			return out
		}
	}

	interface PathToken
	data class PathTokenNumber(val value: Double) : PathToken
	data class PathTokenCmd(val id: Char) : PathToken

	data class SvgStyle(
		val styles: MutableMap<String, String> = hashMapOf()
	) {
		companion object {
			fun tokenize(str: String): List<String> {
				val sr = StrReader(str)
				val out = arrayListOf<String>()
				while (sr.hasMore) {
					while (true) {
						sr.skipSpaces()
						val id = sr.readWhile { it.isLetterOrUnderscore() || it.isNumeric || it == '-' || it == '#' }
						if (id.isNotEmpty()) {
							out += id
						} else {
							break
						}
					}
					if (sr.eof) break
					sr.skipSpaces()
					val symbol = sr.read()
					out += "$symbol"
				}
				return out
			}

			fun ListReader<String>.readId() = this.read()
			fun ListReader<String>.readColon() = expect(":")
			fun ListReader<String>.readExpression() = this.read()

			fun parse(str: String, warningProcessor: ((message: String) -> Unit)? = null): SvgStyle {
				val tokens = tokenize(str)
				val tr = ListReader(tokens)
				//println("Style: $str : $tokens")
				val style = SvgStyle()
				while (tr.hasMore) {
					val id = tr.readId()
					if (tr.eof) {
                        warningProcessor?.invoke("EOF. Parsing (ID='$id'): '$str', $tokens")
						break
					}
					tr.readColon()
					val rexpr = arrayListOf<String>()
					while (tr.hasMore && tr.peek() != ";") {
						rexpr += tr.readExpression()
					}
					style.styles[id.toLowerCase()] = rexpr.joinToString("")
					if (tr.hasMore) tr.expect(";")
					//println("$id --> $rexpr")
				}
				return style
			}
		}
	}
}
