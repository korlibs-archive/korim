package com.soywiz.korim.vector

import com.soywiz.korio.ds.DoubleArrayList
import com.soywiz.korio.ds.IntArrayList
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.math.Vector2

class GraphicsPath(
	val commands: IntArrayList = IntArrayList(),
	val data: DoubleArrayList = DoubleArrayList(),
	val winding: Winding = Winding.EVEN_ODD
) : Context2d.SizedDrawable {

	override val width: Int get() = this.getBounds().width.toInt()
	override val height: Int get() = this.getBounds().height.toInt()

	override fun draw(c: Context2d) {
		c.state.path.write(this)
	}

	fun clone() = GraphicsPath(IntArrayList(commands), DoubleArrayList(data), winding)

	interface Visitor {
		fun close()
		fun moveTo(x: Double, y: Double)
		fun lineTo(x: Double, y: Double)
		fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double)
		fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double)
	}

	inline fun visitCmds(
		moveTo: (x: Double, y: Double) -> Unit,
		lineTo: (x: Double, y: Double) -> Unit,
		quadTo: (x1: Double, y1: Double, x2: Double, y2: Double) -> Unit,
		bezierTo: (x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) -> Unit,
		close: () -> Unit
	) {
		var n = 0
		for (cmd in commands) {
			when (cmd) {
				Command.MOVE_TO -> {
					val x = data[n++]
					val y = data[n++]
					moveTo(x, y)
				}
				Command.LINE_TO -> {
					val x = data[n++]
					val y = data[n++]
					lineTo(x, y)
				}
				Command.QUAD_TO -> {
					val x1 = data[n++]
					val y1 = data[n++]
					val x2 = data[n++]
					val y2 = data[n++]
					quadTo(x1, y1, x2, y2)
				}
				Command.BEZIER_TO -> {
					val x1 = data[n++]
					val y1 = data[n++]
					val x2 = data[n++]
					val y2 = data[n++]
					val x3 = data[n++]
					val y3 = data[n++]
					bezierTo(x1, y1, x2, y2, x3, y3)
				}
				Command.CLOSE -> {
					close()
				}
			}
		}
	}

	inline fun visitEdges(
		line: (x0: Double, y0: Double, x1: Double, y1: Double) -> Unit,
		quad: (x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double) -> Unit,
		bezier: (x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) -> Unit,
		close: () -> Unit
	) {
		var mx = 0.0
		var my = 0.0
		var lx = 0.0
		var ly = 0.0
		visitCmds(
			moveTo = { x, y ->
				mx = x
				my = y
				lx = x
				ly = y
			},
			lineTo = { x, y ->
				line(lx, ly, x, y)
				lx = x
				ly = y
			},
			quadTo = { x1, y1, x2, y2 ->
				quad(lx, ly, x1, y1, x2, y2)
				lx = x2
				ly = y2
			},
			bezierTo = { x1, y1, x2, y2, x3, y3 ->
				bezier(lx, ly, x1, y1, x2, y2, x3, y3)
				lx = x3
				ly = y3
			},
			close = {
				if ((lx != mx) || (ly != my)) {
					line(lx, ly, mx, my)
				}
				close()
			}
		)
	}

	fun visit(visitor: Visitor) {
		visitCmds(
			moveTo = visitor::moveTo,
			lineTo = visitor::lineTo,
			quadTo = visitor::quadTo,
			bezierTo = visitor::cubicTo,
			close = visitor::close
		)
	}

	fun isEmpty(): Boolean = commands.isEmpty()
	fun isNotEmpty(): Boolean = commands.isNotEmpty()

	fun clear() {
		commands.clear()
		data.clear()
	}

	private var lastX = 0.0
	private var lastY = 0.0

	fun moveTo(p: Vector2) = moveTo(p.x, p.y)
	fun lineTo(p: Vector2) = lineTo(p.x, p.y)

	fun moveTo(x: Double, y: Double) {
		commands += Command.MOVE_TO
		data += x
		data += y
		lastX = x
		lastY = y
	}

	fun moveTo(x: Int, y: Int) = moveTo(x.toDouble(), y.toDouble())

	fun moveToH(x: Double) = moveTo(x, lastY)
	fun rMoveToH(x: Double) = moveTo(lastX + x, lastY)

	fun moveToV(y: Double) = moveTo(lastX, y)
	fun rMoveToV(y: Double) = moveTo(lastX, lastY + y)

	fun rMoveTo(x: Double, y: Double) = moveTo(this.lastX + x, this.lastY + y)
	fun rLineTo(x: Double, y: Double) = lineTo(this.lastX + x, this.lastY + y)

	fun rQuadTo(cx: Double, cy: Double, ax: Double, ay: Double) = quadTo(this.lastX + cx, this.lastY + cy, this.lastX + ax, this.lastY + ay)
	fun rCubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) = cubicTo(this.lastX + cx1, this.lastY + cy1, this.lastX + cx2, this.lastY + cy2, this.lastX + ax, this.lastY + ay)

	private fun ensureMoveTo(x: Double, y: Double) {
		if (isEmpty()) {
			moveTo(x, y)
		}
	}

	fun lineTo(x: Double, y: Double) {
		ensureMoveTo(x, y)
		commands += Command.LINE_TO
		data += x
		data += y
		lastX = x
		lastY = y
	}

	fun lineTo(x: Int, y: Int) = lineTo(x.toDouble(), y.toDouble())

	fun quadTo(controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) {
		ensureMoveTo(controlX, controlY)
		commands += Command.QUAD_TO
		data += controlX
		data += controlY
		data += anchorX
		data += anchorY
		lastX = anchorX
		lastY = anchorY
	}

	fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) {
		ensureMoveTo(cx1, cy1)
		commands += Command.BEZIER_TO
		data += cx1
		data += cy1
		data += cx2
		data += cy2
		data += ax
		data += ay
		lastX = ax
		lastY = ay
	}

	//fun arcTo(b: Point2d, a: Point2d, c: Point2d, r: Double) {
	fun arcTo(ax: Double, ay: Double, cx: Double, cy: Double, r: Double) {
		ensureMoveTo(ax, ay)
		val bx = lastX
		val by = lastY
		val b = Vector2(bx, by)
		val a = Vector2(ax, ay)
		val c = Vector2(cx, cy)
		val PI_DIV_2 = Math.PI / 2.0
		val AB = b - a
		val AC = c - a
		val angle = Vector2.angle(AB, AC) * 0.5
		val x = r * Math.sin(PI_DIV_2 - angle) / Math.sin(angle)
		val A = a + AB.unit * x
		val B = a + AC.unit * x
		lineTo(A.x, A.y)
		quadTo(a.x, a.y, B.x, B.y)
	}

	fun close() {
		commands += Command.CLOSE
	}

	fun rect(x: Double, y: Double, width: Double, height: Double) {
		moveTo(x, y)
		lineTo(x + width, y)
		lineTo(x + width, y + height)
		lineTo(x, y + height)
		close()
	}

	fun arc(x: Double, y: Double, r: Double, start: Double, end: Double) {
		// http://hansmuller-flex.blogspot.com.es/2011/04/approximating-circular-arc-with-cubic.html
		val EPSILON = 0.00001
		val PI_TWO = Math.PI * 2
		val PI_OVER_TWO = Math.PI / 2.0

		val startAngle = start % PI_TWO
		val endAngle = end % PI_TWO
		var remainingAngle = Math.min(PI_TWO, Math.abs(endAngle - startAngle))
		if (remainingAngle == 0.0 && start != end) remainingAngle = PI_TWO
		val sgn = if (startAngle < endAngle) 1 else -1
		var a1 = startAngle
		val p1 = Vector2();
		val p2 = Vector2();
		val p3 = Vector2();
		val p4 = Vector2()
		var index = 0
		while (remainingAngle > EPSILON) {
			val a2 = a1 + sgn * Math.min(remainingAngle, PI_OVER_TWO)

			val k = 0.5522847498
			val a = (a2 - a1) / 2.0
			val x4 = r * Math.cos(a)
			val y4 = r * Math.sin(a)
			val x1 = x4
			val y1 = -y4
			val f = k * Math.tan(a)
			val x2 = x1 + f * y4
			val y2 = y1 + f * x4
			val x3 = x2
			val y3 = -y2
			val ar = a + a1
			val cos_ar = Math.cos(ar);
			val sin_ar = Math.sin(ar)
			p1.setTo(x + r * Math.cos(a1), y + r * Math.sin(a1))
			p2.setTo(x + x2 * cos_ar - y2 * sin_ar, y + x2 * sin_ar + y2 * cos_ar)
			p3.setTo(x + x3 * cos_ar - y3 * sin_ar, y + x3 * sin_ar + y3 * cos_ar)
			p4.setTo(x + r * Math.cos(a2), y + r * Math.sin(a2))

			if (index == 0) moveTo(p1.x, p1.y)
			cubicTo(p2.x, p2.y, p3.x, p3.y, p4.x, p4.y)

			index++
			remainingAngle -= Math.abs(a2 - a1)
			a1 = a2
		}
		if (startAngle == endAngle && index != 0) {
			close()
		}
	}

	fun getBounds(out: Rectangle = Rectangle()): Rectangle {
		return synchronized(GetBounds) {
			GetBounds.reset()
			visit(GetBounds)
			GetBounds.bb.getBounds(out)
		}
	}

	// http://erich.realtimerendering.com/ptinpoly/
	// http://stackoverflow.com/questions/217578/how-can-i-determine-whether-a-2d-point-is-within-a-polygon/2922778#2922778
	// https://www.particleincell.com/2013/cubic-line-intersection/
	// I run a semi-infinite ray horizontally (increasing x, fixed y) out from the test point, and count how many edges it crosses.
	// At each crossing, the ray switches between inside and outside. This is called the Jordan curve theorem.
	fun containsPoint(x: Double, y: Double): Boolean {
		val testx = x
		val testy = y

		var intersects = false

		visitEdges(
			line = { x0, y0, x1, y1 ->
				if (intersectsH0LineLine(testx, testy, x0, y0, x1, y1)) intersects = !intersects
			},
			quad = { x0, y0, x1, y1, x2, y2 ->
				if (intersectsH0LineBezier(testx, testy, x0, y0, x1, y1, x1, y1, x2, y2)) intersects = !intersects
			},
			bezier = { x0, y0, x1, y1, x2, y2, x3, y3 ->
				if (intersectsH0LineBezier(testx, testy, x0, y0, x1, y1, x2, y2, x3, y3)) intersects = !intersects
			},
			close = {
			}
		)
		return intersects
	}

	fun containsPoint(x: Int, y: Int): Boolean = containsPoint(x.toDouble(), y.toDouble())

	private fun intersectsH0LineLine(
		testx: Double, testy: Double,
		bx0: Double, by0: Double, bx1: Double, by1: Double
	): Boolean {
		return ((by1 > testy) != (by0 > testy)) && (testx < (bx0 - bx1) * (testy - by1) / (by0 - by1) + bx1)
	}

	private fun intersectsH0LineBezier(
		ax: Double, ay: Double,
		bx0: Double, by0: Double, bx1: Double, by1: Double, bx2: Double, by2: Double, bx3: Double, by3: Double
	): Boolean {
		// @TODO: Proper bezier intersection
		return intersectsH0LineLine(ax, ay, bx0, by0, bx2, by2)
	}

	//private fun intersectsLineLine(
	//	ax0: Double, ay0: Double, ax1: Double, ay1: Double,
	//	bx0: Double, by0: Double, bx1: Double, by1: Double
	//): Boolean {
	//	var intersects = false
	//	return intersects
	//}
//
	//private fun intersectsLineBezier(
	//	ax0: Double, ay0: Double, ax1: Double, ay1: Double,
	//	bx0: Double, by0: Double, bx1: Double, by1: Double, bx2: Double, by2: Double, bx3: Double, by3: Double
	//): Boolean {
	//	// @TODO: Proper bezier intersection
	//	return intersectsLineLine(ax0, ay0, ax1, ay1, bx0, by0, bx2, by2)
	//}

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

		override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) {
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

	fun write(path: GraphicsPath) {
		this.commands += path.commands
		this.data += path.data
		this.lastX = path.lastX
		this.lastY = path.lastY
	}
}

