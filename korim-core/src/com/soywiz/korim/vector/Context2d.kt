package com.soywiz.korim.vector

import com.soywiz.korim.color.Colors
import com.soywiz.korim.geom.Matrix2d
import com.soywiz.korim.geom.Point2d
import com.soywiz.korim.geom.Vector2
import java.util.*

open class Context2d {
	enum class LineCap { BUTT, ROUND, SQUARE }
	enum class LineJoin { BEVEL, MITER, ROUND }

	class State(
		var transform: Matrix2d = Matrix2d(),
		var clip: GraphicsPath? = null,
		var path: GraphicsPath = GraphicsPath(),
		var lineWidth: Double = 1.0,
		var lineCap: LineCap = LineCap.BUTT,
		var lineJoin: LineJoin = LineJoin.MITER,
		var miterLimit: Double = 10.0,
		var strokeStyle: Paint = Color(Colors.BLACK),
		var fillStyle: Paint = Color(Colors.BLACK)
	) {
		fun clone(): State = State(
			transform = transform.clone(),
			clip = clip?.clone(),
			path = path.clone(),
			lineWidth = lineWidth,
			lineCap = lineCap,
			lineJoin = lineJoin,
			miterLimit = miterLimit,
			strokeStyle = strokeStyle,
			fillStyle = fillStyle
		)
	}

	private var state = State()
	private val stack = LinkedList<State>()

	var lineWidth: Double; get() = state.lineWidth; set(value) = run { state.lineWidth = value }
	var lineCap: LineCap; get() = state.lineCap; set(value) = run { state.lineCap = value }
	var strokeStyle: Paint; get() = state.strokeStyle; set(value) = run { state.strokeStyle = value }
	var fillStyle: Paint; get() = state.fillStyle; set(value) = run { state.fillStyle = value }

	interface Paint
	class Color(val color: Int) : Paint

	inline fun keep(callback: () -> Unit) {
		save()
		try {
			callback()
		} finally {
			restore()
		}
	}

	fun save() {
		stack.add(state.clone())
	}

	fun restore() {
		state = stack.removeLast()
	}

	fun scale(sx: Double, sy: Double = sx) {
		state.transform.prescale(sx, sy)
	}

	fun rotate(angle: Double) {
		state.transform.prerotate(angle)
	}

	fun translate(tx: Double, ty: Double) {
		state.transform.pretranslate(tx, ty)
	}

	fun transform(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double) {
		state.transform.premulitply(a, b, c, d, tx, ty)
	}

	fun setTransform(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double) {
		state.transform.setTo(a, b, c, d, tx, ty)
	}

	fun shear(sx: Double, sy: Double) = transform(1.0, sy, sx, 1.0, 0.0, 0.0)

	private var px: Double = 0.0
	private var py: Double = 0.0

	private var lastMx: Double = 0.0
	private var lastMy: Double = 0.0

	fun moveTo(x: Int, y: Int) = moveTo(x.toDouble(), y.toDouble())
	fun lineTo(x: Int, y: Int) = lineTo(x.toDouble(), y.toDouble())
	fun quadraticCurveTo(cx: Int, cy: Int, ax: Int, ay: Int) = quadraticCurveTo(cx.toDouble(), cy.toDouble(), ax.toDouble(), ay.toDouble())
	fun bezierCurveTo(cx1: Int, cy1: Int, cx2: Int, cy2: Int, ax: Int, ay: Int) = bezierCurveTo(cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())
	fun arcTo(x1: Int, y1: Int, x2: Int, y2: Int, radius: Int) = arcTo(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(), radius.toDouble())

	fun moveTo(p: Vector2) = moveTo(p.x, p.y)
	fun lineTo(p: Vector2) = lineTo(p.x, p.y)
	fun quadraticCurveTo(c: Vector2, a: Vector2) = quadraticCurveTo(c.x, c.y, a.x, a.y)
	fun bezierCurveTo(c1: Vector2, c2: Vector2, a: Vector2) = bezierCurveTo(c1.x, c1.y, c2.x, c2.y, a.x, a.y)
	fun arcTo(p1: Vector2, p2: Vector2, radius: Double) = arcTo(p1.x, p1.y, p2.x, p2.y, radius)

	fun rect(x: Int, y: Int, width: Int, height: Int) = rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
	fun strokeRect(x: Int, y: Int, width: Int, height: Int) = strokeRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
	fun fillRect(x: Int, y: Int, width: Int, height: Int) = fillRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

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
		val p1 = Vector2(); val p2 = Vector2(); val p3 = Vector2(); val p4 = Vector2()
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
			val cos_ar = Math.cos(ar) ; val sin_ar = Math.sin(ar)
			p1.setTo(x + r * Math.cos(a1), y + r * Math.sin(a1))
			p2.setTo(x + x2 * cos_ar - y2 * sin_ar, y + x2 * sin_ar + y2 * cos_ar)
			p3.setTo(x + x3 * cos_ar - y3 * sin_ar, y + x3 * sin_ar + y3 * cos_ar)
			p4.setTo(x + r * Math.cos(a2), y + r * Math.sin(a2))

			if (index == 0) moveTo(p1)
			bezierCurveTo(p2, p3, p4)

			index++
			remainingAngle -= Math.abs(a2 - a1)
			a1 = a2
		}
		if (startAngle == endAngle && index != 0) {
			closePath()
		}
	}

	fun arcTo(b: Point2d, a: Point2d, c: Point2d, r: Double) {
		val PI_DIV_2 = Math.PI / 2.0
		val AB = b - a
		val AC = c - a
		val angle = Vector2.angle(AB, AC) * 0.5
		val x = r * Math.sin(PI_DIV_2 - angle) / Math.sin(angle)
		val A = a + AB.unit * x
		val B = a + AC.unit * x
		lineTo(A)
		quadraticCurveTo(a, B)
	}

	fun strokeDot(x: Double, y: Double) {
		moveTo(x, y)
		lineTo(x, y)
		stroke()
	}

	fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, r: Double) {
		arcTo(Point2d(px, py), Point2d(x1, y1), Point2d(x2, y2), r)
	}

	fun circle(x: Double, y: Double, radius: Double) = arc(x, y, radius, 0.0, Math.PI * 2.0)

	fun moveTo(x: Double, y: Double) {
		state.path.moveTo(x, y)
		px = x
		py = y
		lastMx = x
		lastMy = y
	}

	fun lineTo(x: Double, y: Double) {
		if (state.path.isEmpty()) state.path.moveTo(px, py)
		state.path.lineTo(x, y)
		px = x
		py = y
	}

	fun quadraticCurveTo(cx: Double, cy: Double, ax: Double, ay: Double) {
		if (state.path.isEmpty()) state.path.moveTo(px, py)
		state.path.quadTo(cx, cy, ax, ay)
		px = ax
		py = ay
	}

	fun bezierCurveTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, x: Double, y: Double) {
		if (state.path.isEmpty()) state.path.moveTo(px, py)
		state.path.curveTo(cx1, cy1, cx2, cy2, x, y)
		px = x
		py = y
	}

	fun rect(x: Double, y: Double, width: Double, height: Double) {
		moveTo(x, y)
		lineTo(x + width, y)
		lineTo(x + width, y + height)
		lineTo(x, y + height)
		closePath()
	}

	fun strokeRect(x: Double, y: Double, width: Double, height: Double) {
		beginPath()
		rect(x, y, width, height)
		stroke()
	}

	fun fillRect(x: Double, y: Double, width: Double, height: Double) {
		beginPath()
		rect(x, y, width, height)
		fill()
	}

	fun beginPath() {
		state.path = GraphicsPath()
	}

	fun closePath() {
		state.path.close()
		px = lastMx
		py = lastMy
	}

	fun stroke() {
		render(state, fill = false)
	}

	fun fill() {
		render(state, fill = true)
	}

	fun clip() {
		state.clip = state.path
	}

	open fun render(state: State, fill: Boolean) {
	}
}

