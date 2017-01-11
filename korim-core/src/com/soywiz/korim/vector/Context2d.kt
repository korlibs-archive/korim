package com.soywiz.korim.vector

import com.soywiz.korim.color.Colors
import com.soywiz.korim.geom.Matrix2d
import com.soywiz.korim.geom.Vector2
import java.util.*

class Context2d(val renderer: Renderer) {
	enum class LineCap { BUTT, ROUND, SQUARE }
	enum class LineJoin { BEVEL, MITER, ROUND }

	interface Renderer {
		fun render(state: State, fill: Boolean)
	}

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

	internal var state = State()
	private val stack = LinkedList<State>()

	var lineWidth: Double; get() = state.lineWidth; set(value) = run { state.lineWidth = value }
	var lineCap: LineCap; get() = state.lineCap; set(value) = run { state.lineCap = value }
	var strokeStyle: Paint; get() = state.strokeStyle; set(value) = run { state.strokeStyle = value }
	var fillStyle: Paint; get() = state.fillStyle; set(value) = run { state.fillStyle = value }

	interface Paint
	data class Color(val color: Int) : Paint
	object None : Paint

	inline fun keepApply(callback: Context2d.() -> Unit) = this.apply { keep { callback() } }

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
		state.path.arc(x, y, r, start, end)
	}

	fun strokeDot(x: Double, y: Double) {
		beginPath()
		moveTo(x, y)
		lineTo(x, y)
		stroke()
	}

	fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, r: Double) {
		state.path.arcTo(x1, y1, x2, y2, r)
	}

	fun circle(x: Double, y: Double, radius: Double) = arc(x, y, radius, 0.0, Math.PI * 2.0)

	fun moveTo(x: Double, y: Double) {
		state.path.moveTo(x, y)
	}

	fun lineTo(x: Double, y: Double) {
		state.path.lineTo(x, y)
	}

	fun quadraticCurveTo(cx: Double, cy: Double, ax: Double, ay: Double) {
		state.path.quadTo(cx, cy, ax, ay)
	}

	fun bezierCurveTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, x: Double, y: Double) {
		state.path.cubicTo(cx1, cy1, cx2, cy2, x, y)
	}

	fun rect(x: Double, y: Double, width: Double, height: Double) {
		state.path.rect(x, y, width, height)
	}

	fun path(path: GraphicsPath) {
		this.state.path.write(path)
	}

	fun draw(d: Drawable) {
		d.draw(this)
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
	}

	fun stroke() {
		if (state.strokeStyle != None) renderer.render(state, fill = false)
	}

	fun fill() {
		if (state.fillStyle != None) renderer.render(state, fill = true)
	}

	fun fillStroke() {
		fill()
		stroke()
	}

	fun clip() {
		state.clip = state.path
	}

	interface Drawable {
		fun draw(c: Context2d)
	}

	class FuncDrawable(val action: Context2d.() -> Unit) : Context2d.Drawable {
		override fun draw(c: Context2d) {
			c.keep {
				action(c)
			}
		}
	}
}

