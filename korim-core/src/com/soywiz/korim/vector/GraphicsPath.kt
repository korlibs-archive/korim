package com.soywiz.korim.vector

import com.soywiz.korim.geom.Vector2
import com.soywiz.korim.geom.Rectangle
import java.util.*

class GraphicsPath(
	val commands: ArrayList<Int> = arrayListOf<Int>(),
	val data: ArrayList<Double> = arrayListOf<Double>(),
	val winding: Winding = Winding.EVEN_ODD
) {
	fun clone() = GraphicsPath(ArrayList(commands), ArrayList(data), winding)

	interface Visitor {
		fun close()
		fun moveTo(x: Double, y: Double)
		fun lineTo(x: Double, y: Double)
		fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double)
		fun bezierTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double)
	}

	fun visit(visitor: Visitor) {
		var n = 0
		for (cmd in commands) {
			when (cmd) {
				Command.MOVE_TO -> {
					val x = data[n++]
					val y = data[n++]
					visitor.moveTo(x, y)
				}
				Command.LINE_TO -> {
					val x = data[n++]
					val y = data[n++]
					visitor.lineTo(x, y)
				}
				Command.QUAD_TO -> {
					val x1 = data[n++]
					val y1 = data[n++]
					val x2 = data[n++]
					val y2 = data[n++]
					visitor.quadTo(x1, y1, x2, y2)
				}
				Command.BEZIER_TO -> {
					val x1 = data[n++]
					val y1 = data[n++]
					val x2 = data[n++]
					val y2 = data[n++]
					val x3 = data[n++]
					val y3 = data[n++]
					visitor.bezierTo(x1, y1, x2, y2, x3, y3)
				}
				Command.CLOSE -> {
					visitor.close()
				}
			}
		}

	}

	fun isEmpty(): Boolean = commands.isEmpty()
	fun isNotEmpty(): Boolean = commands.isNotEmpty()

	fun clear() {
		commands.clear()
		data.clear()
	}

	fun moveTo(x: Double, y: Double) {
		commands += Command.MOVE_TO
		data += x
		data += y
	}

	fun lineTo(x: Double, y: Double) {
		commands += Command.LINE_TO
		data += x
		data += y
	}

	fun quadTo(controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) {
		commands += Command.QUAD_TO
		data += controlX
		data += controlY
		data += anchorX
		data += anchorY
	}

	fun curveTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) {
		commands += Command.BEZIER_TO
		data += cx1
		data += cy1
		data += cx2
		data += cy2
		data += ax
		data += ay
	}

	fun close() {
		commands += Command.CLOSE
	}

	fun getBounds(out: Rectangle = Rectangle()): Rectangle {
		return synchronized(GetBounds) {
			GetBounds.reset()
			visit(GetBounds)
			GetBounds.bb.getBounds()
		}
	}

	object GetBounds : Visitor {
		val bb = BoundsBuilder()
		val temp = Rectangle()
		var lx = 0.0
		var ly = 0.0

		fun reset() {
			bb.reset()
			temp.setTo(0.0, 0.0, 0.0, 0.0)
			lx = 0.0
			ly = 0.0
		}

		override fun moveTo(x: Double, y: Double) {
			bb.add(x, y)
			lx = x
			ly = y
		}

		override fun lineTo(x: Double, y: Double) {
			bb.add(x, y)
			lx = x
			ly = y
		}

		override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) {
			bb.add(CurveBounds.quadMinMax(lx, ly, cx, cy, ax, ay, temp))
			lx = ax
			ly = ay
		}

		override fun bezierTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) {
			bb.add(CurveBounds.bezierMinMax(lx, ly, cx1, cy1, cx2, cy2, ax, ay, temp))
			lx = ax
			ly = ay
		}

		override fun close() {
		}
	}

	object Command {
		//val CUBIC_CURVE_TO = 6
		val MOVE_TO = 1
		val LINE_TO = 2
		val QUAD_TO = 3
		val BEZIER_TO = 4
		val CLOSE = 5
		//val NO_OP = 0
		//val WIDE_LINE_TO = 5
		//val WIDE_MOVE_TO = 4
	}

	enum class Winding(val str: String) {
		EVEN_ODD("evenOdd"), NON_ZERO("nonZero");
	}

	//(x0,y0) is start point; (x1,y1),(x2,y2) is control points; (x3,y3) is end point.
	object CurveBounds {
		private val tvalues = DoubleArray(6)
		private val xvalues = DoubleArray(8)
		private val yvalues = DoubleArray(8)

		fun quadMinMax(x0: Double, y0: Double, xc: Double, yc: Double, x1: Double, y1: Double, target: Rectangle = Rectangle()): Rectangle {
			// http://fontforge.github.io/bezier.html
			//Any quadratic spline can be expressed as a cubic (where the cubic term is zero). The end points of the cubic will be the same as the quadratic's.
			//CP0 = QP0
			//CP3 = QP2
			//The two control points for the cubic are:
			//CP1 = QP0 + 2/3 *(QP1-QP0)
			//CP2 = QP2 + 2/3 *(QP1-QP2)

			//return bezierMinMax(x0, y0, xc, yc, xc, yc, x1, y1, target)
			return bezierMinMax(
				x0, y0,
				x0 + 2 / 3 * (xc - x0), y0 + 2 / 3 * (yc - y0),
				x1 + 2 / 3 * (xc - x1), y1 + 2 / 3 * (yc - y1),
				x1, y1,
				target
			)
		}

		fun bezierMinMax(x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, target: Rectangle = Rectangle()): Rectangle {
			var j = 0
			var a: Double
			var b: Double
			var c: Double
			var b2ac: Double
			var sqrtb2ac: Double
			for (i in 0 until 2) {
				if (i == 0) {
					b = 6 * x0 - 12 * x1 + 6 * x2
					a = -3 * x0 + 9 * x1 - 9 * x2 + 3 * x3
					c = 3 * x1 - 3 * x0
				} else {
					b = 6 * y0 - 12 * y1 + 6 * y2
					a = -3 * y0 + 9 * y1 - 9 * y2 + 3 * y3
					c = 3 * y1 - 3 * y0
				}
				if (Math.abs(a) < 1e-12) {
					if (Math.abs(b) >= 1e-12) {
						val t = -c / b
						if (0 < t && t < 1) tvalues[j++] = t
					}
				} else {
					b2ac = b * b - 4 * c * a
					if (b2ac < 0) continue
					sqrtb2ac = Math.sqrt(b2ac)
					val t1 = (-b + sqrtb2ac) / (2 * a)
					if (0 < t1 && t1 < 1) tvalues[j++] = t1
					val t2 = (-b - sqrtb2ac) / (2 * a)
					if (0 < t2 && t2 < 1) tvalues[j++] = t2
				}
			}

			while (j-- > 0) {
				val t = tvalues[j]
				val mt = 1 - t
				xvalues[j] = (mt * mt * mt * x0) + (3 * mt * mt * t * x1) + (3 * mt * t * t * x2) + (t * t * t * x3)
				yvalues[j] = (mt * mt * mt * y0) + (3 * mt * mt * t * y1) + (3 * mt * t * t * y2) + (t * t * t * y3)
			}

			xvalues[tvalues.size + 0] = x0
			xvalues[tvalues.size + 1] = x3
			yvalues[tvalues.size + 0] = y0
			yvalues[tvalues.size + 1] = y3

			return target.setBounds(xvalues.min() ?: 0.0, yvalues.min() ?: 0.0, xvalues.max() ?: 0.0, yvalues.max() ?: 0.0)
		}
	}

	class BoundsBuilder {
		private val xList = arrayListOf<Double>()
		private val yList = arrayListOf<Double>()

		fun reset() {
			xList.clear()
			yList.clear()
		}

		fun add(x: Double, y: Double) {
			xList += x
			yList += y
		}

		fun add(p: Vector2) = add(p.x, p.y)

		fun add(rect: Rectangle) {
			add(rect.left, rect.top)
			add(rect.bottom, rect.right)
		}

		fun getBounds(out: Rectangle = Rectangle()): Rectangle = out.setBounds(xList.min() ?: 0.0, yList.min() ?: 0.0, xList.max() ?: 0.0, yList.max() ?: 0.0)
	}

	fun Iterable<Vector2>.bounds(out: Rectangle = Rectangle()): Rectangle = out.setBounds(
		left = this.map { it.x }.min() ?: 0.0,
		top = this.map { it.y }.min() ?: 0.0,
		right = this.map { it.x }.max() ?: 0.0,
		bottom = this.map { it.y }.max() ?: 0.0
	)
}

