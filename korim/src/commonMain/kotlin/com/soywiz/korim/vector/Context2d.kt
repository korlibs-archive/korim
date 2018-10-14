package com.soywiz.korim.vector

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import kotlin.math.*

class Context2d(val renderer: Renderer) : Disposable {
	val width: Int get() = renderer.width
	val height: Int get() = renderer.height

	enum class LineCap { BUTT, ROUND, SQUARE }
	enum class LineJoin { BEVEL, MITER, ROUND }
	enum class CycleMethod { NO_CYCLE, REFLECT, REPEAT }

	enum class ShapeRasterizerMethod(val scale: Double) {
		NONE(0.0), X1(1.0), X2(2.0), X4(4.0)
	}

	override fun dispose() {
		renderer.dispose()
	}

	fun withScaledRenderer(scaleX: Double, scaleY: Double = scaleX): Context2d = if (scaleX == 1.0 && scaleY == 1.0) this else Context2d(ScaledRenderer(renderer, scaleX, scaleY))

	class ScaledRenderer(val parent: Renderer, val scaleX: Double, val scaleY: Double) : Renderer() {
		override val width: Int get() = (parent.width / scaleX).toInt()
		override val height: Int get() = (parent.height / scaleY).toInt()

		private inline fun <T> adjustMatrix(transform: Matrix2d, callback: () -> T): T {
			return transform.keep {
				transform.scale(scaleX, scaleY)
				callback()
			}
		}

		private inline fun <T> adjustState(state: State, callback: () -> T): T =
			adjustMatrix(state.transform) { callback() }

		override fun render(state: State, fill: Boolean): Unit = adjustState(state) { parent.render(state, fill) }
		override fun renderText(state: State, font: Font, text: String, x: Double, y: Double, fill: Boolean): Unit =
			adjustState(state) { parent.renderText(state, font, text, x, y, fill) }

		override fun getBounds(font: Font, text: String, out: TextMetrics): Unit = parent.getBounds(font, text, out)
		override fun drawImage(image: Bitmap, x: Int, y: Int, width: Int, height: Int, transform: Matrix2d): Unit {
			adjustMatrix(transform) { parent.drawImage(image, x, y, width, height, transform) }
		}
	}

	abstract class Renderer {
		companion object {
			val DUMMY = object : Renderer() {
				override val width: Int = 128
				override val height: Int = 128
			}
		}

		abstract val width: Int
		abstract val height: Int

		open fun render(state: State, fill: Boolean): Unit = Unit
		open fun renderText(state: State, font: Font, text: String, x: Double, y: Double, fill: Boolean): Unit = Unit
		open fun getBounds(font: Font, text: String, out: TextMetrics): Unit =
			run { out.bounds.setTo(0.0, 0.0, 0.0, 0.0) }

		open fun drawImage(
			image: Bitmap,
			x: Int,
			y: Int,
			width: Int = image.width,
			height: Int = image.height,
			transform: Matrix2d = Matrix2d()
		): Unit {
			val state = State(transform = transform, path = GraphicsPath().apply { rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble()) }, fillStyle = Context2d.BitmapPaint(
				image,
				transform = Matrix2d()
					.scale(width.toDouble() / image.width.toDouble(), height.toDouble() / image.height.toDouble())
					.translate(x, y)
			))
			render(state, fill = true)
		}

		open fun dispose(): Unit {
		}
	}

	enum class VerticalAlign(val ratio: Double) {
		TOP(0.0), MIDDLE(0.5), BASELINE(1.0), BOTTOM(1.0);

		fun getOffsetY(height: Double, baseline: Double): Double = when (this) {
			BASELINE -> baseline
			else -> -height * ratio
		}
	}

	enum class HorizontalAlign(val ratio: Double) {
		LEFT(0.0), CENTER(0.5), RIGHT(1.0);

		fun getOffsetX(width: Double): Double = width * ratio
	}

	enum class ScaleMode(val hScale: Boolean, val vScale: Boolean) {
		NONE(false, false), HORIZONTAL(true, false), VERTICAL(false, true), NORMAL(true, true);
	}

	data class State(
		var transform: Matrix2d = Matrix2d(),
		var clip: GraphicsPath? = null,
		var path: GraphicsPath = GraphicsPath(),
		var lineScaleMode: ScaleMode = ScaleMode.NORMAL,
		var lineWidth: Double = 1.0,
		var lineCap: LineCap = LineCap.BUTT,
		var lineJoin: LineJoin = LineJoin.MITER,
		var miterLimit: Double = 10.0,
		var strokeStyle: Paint = Color(Colors.BLACK),
		var fillStyle: Paint = Color(Colors.BLACK),
		var font: Font = Font("sans-serif", 10.0),
		var verticalAlign: VerticalAlign = VerticalAlign.BASELINE,
		var horizontalAlign: HorizontalAlign = HorizontalAlign.LEFT,
		var globalAlpha: Double = 1.0
	) {
		fun clone(): State = this.copy(
			transform = transform.clone(),
			clip = clip?.clone(),
			path = path.clone()
		)
	}

	var state = State()
	private val stack = LinkedList<State>()

	var lineScaleMode: ScaleMode by redirectField { state::lineScaleMode }
	var lineWidth: Double by redirectField { state::lineWidth }
	var lineCap: LineCap by redirectField { state::lineCap }
	var strokeStyle: Paint by redirectField { state::strokeStyle }
	var fillStyle: Paint by redirectField { state::fillStyle }
	var font: Font by redirectField { state::font }
	var verticalAlign: VerticalAlign by redirectField { state::verticalAlign }
	var horizontalAlign: HorizontalAlign by redirectField { state::horizontalAlign }
	var globalAlpha: Double by redirectField { state::globalAlpha }

	inline fun fillStyle(paint: Paint, callback: () -> Unit) {
		val oldStyle = fillStyle
		fillStyle = paint
		try {
			callback()
		} finally {
			fillStyle = oldStyle
		}
	}

	inline fun strokeStyle(paint: Paint, callback: () -> Unit) {
		val oldStyle = strokeStyle
		strokeStyle = paint
		try {
			callback()
		} finally {
			strokeStyle = oldStyle
		}
	}

	inline fun font(font: Font?, halign: HorizontalAlign? = null, valign: VerticalAlign? = null, callback: () -> Unit) {
		val oldFont = this.font
		val oldHalign = this.horizontalAlign
		val oldValign = this.verticalAlign
		try {
			if (font != null) this.font = font
			if (halign != null) this.horizontalAlign = halign
			if (valign != null) this.verticalAlign = valign
			callback()
		} finally {
			this.font = oldFont
			this.horizontalAlign = oldHalign
			this.verticalAlign = oldValign
		}
	}

	inline fun fillStyle(color: RGBA, callback: () -> Unit) = fillStyle(createColor(color), callback)

	inline fun keepApply(callback: Context2d.() -> Unit) = this.apply { keep { callback() } }

	inline fun keep(callback: () -> Unit) {
		save()
		try {
			callback()
		} finally {
			restore()
		}
	}

	inline fun keepTransform(callback: () -> Unit) {
		val t = state.transform
		val a = t.a
		val b = t.b
		val c = t.c
		val d = t.d
		val tx = t.tx
		val ty = t.ty
		try {
			callback()
		} finally {
			t.setTo(a, b, c, d, tx, ty)
		}
	}

	fun save() = run { stack.addLast(state.clone()) }
	fun restore() = run { state = stack.removeLast() }

	inline fun scale(sx: Number, sy: Number = sx) = scale(sx.toDouble(), sy.toDouble())
	inline fun translate(tx: Number, ty: Number) = translate(tx.toDouble(), ty.toDouble())
	inline fun rotate(angle: Number) = rotate(angle.toDouble())
	inline fun rotateDeg(degs: Number) = rotateDeg(degs.toDouble())

	fun scale(sx: Double, sy: Double = sx) = run { state.transform.prescale(sx, sy) }
	fun rotate(angle: Double) = run { state.transform.prerotate(angle) }
	fun rotateDeg(degs: Double) = run { state.transform.prerotate(Angle.degreesToRadians(degs)) }

	fun translate(tx: Double, ty: Double) = run { state.transform.pretranslate(tx, ty) }
	fun transform(m: Matrix2d) = run { state.transform.premultiply(m) }
	fun transform(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double) =
		run { state.transform.premultiply(a, b, c, d, tx, ty) }

	fun setTransform(m: Matrix2d) = run { state.transform.copyFrom(m) }
	fun setTransform(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double) =
		run { state.transform.setTo(a, b, c, d, tx, ty) }

	fun shear(sx: Double, sy: Double) = transform(1.0, sy, sx, 1.0, 0.0, 0.0)
	inline fun moveTo(x: Number, y: Number) = moveTo(x.toDouble(), y.toDouble())
	inline fun lineTo(x: Number, y: Number) = lineTo(x.toDouble(), y.toDouble())
	inline fun quadraticCurveTo(cx: Number, cy: Number, ax: Number, ay: Number) =
		quadraticCurveTo(cx.toDouble(), cy.toDouble(), ax.toDouble(), ay.toDouble())

	inline fun bezierCurveTo(cx1: Number, cy1: Number, cx2: Number, cy2: Number, ax: Number, ay: Number) =
		bezierCurveTo(cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())

	inline fun arcTo(x1: Number, y1: Number, x2: Number, y2: Number, radius: Number) =
		arcTo(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(), radius.toDouble())

	fun moveTo(p: Vector2) = moveTo(p.x, p.y)
	fun lineTo(p: Vector2) = lineTo(p.x, p.y)
	fun quadraticCurveTo(c: Vector2, a: Vector2) = quadraticCurveTo(c.x, c.y, a.x, a.y)
	fun bezierCurveTo(c1: Vector2, c2: Vector2, a: Vector2) = bezierCurveTo(c1.x, c1.y, c2.x, c2.y, a.x, a.y)
	fun arcTo(p1: Vector2, p2: Vector2, radius: Double) = arcTo(p1.x, p1.y, p2.x, p2.y, radius)

	inline fun rect(x: Number, y: Number, width: Number, height: Number) =
		rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

	inline fun strokeRect(x: Number, y: Number, width: Number, height: Number) =
		strokeRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

	inline fun fillRect(x: Number, y: Number, width: Number, height: Number) =
		fillRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

	inline fun fillRoundRect(x: Number, y: Number, width: Number, height: Number, rx: Number, ry: Number = rx) {
		beginPath()
		roundRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(), rx.toDouble(), ry.toDouble())
		fill()
	}

	fun arc(x: Double, y: Double, r: Double, start: Double, end: Double) = run { state.path.arc(x, y, r, start, end) }
	fun strokeDot(x: Double, y: Double) = run { beginPath(); moveTo(x, y); lineTo(x, y); stroke() }
	fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, r: Double) = run { state.path.arcTo(x1, y1, x2, y2, r) }
	fun circle(x: Double, y: Double, radius: Double) = arc(x, y, radius, 0.0, PI * 2.0)
	fun rMoveTo(x: Double, y: Double) = run { state.path.rMoveTo(x, y) }
	fun moveTo(x: Double, y: Double) = run { state.path.moveTo(x, y) }
	fun moveToH(x: Double) = run { state.path.moveToH(x) }
	fun moveToV(y: Double) = run { state.path.moveToV(y) }
	fun rMoveToH(x: Double) = run { state.path.rMoveToH(x) }
	fun rMoveToV(y: Double) = run { state.path.rMoveToV(y) }

	fun lineToH(x: Double) = run { state.path.lineToH(x) }
	fun lineToV(y: Double) = run { state.path.lineToV(y) }
	fun rLineToH(x: Double) = run { state.path.rLineToH(x) }
	fun rLineToV(y: Double) = run { state.path.rLineToV(y) }

	fun lineTo(x: Double, y: Double) = run { state.path.lineTo(x, y) }
	fun rLineTo(x: Double, y: Double) = run { state.path.rLineTo(x, y) }
	fun quadraticCurveTo(cx: Double, cy: Double, ax: Double, ay: Double) = run { state.path.quadTo(cx, cy, ax, ay) }
	fun rQuadraticCurveTo(cx: Double, cy: Double, ax: Double, ay: Double) = run { state.path.rQuadTo(cx, cy, ax, ay) }
	fun bezierCurveTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, x: Double, y: Double) =
		run { state.path.cubicTo(cx1, cy1, cx2, cy2, x, y) }

	fun rBezierCurveTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, x: Double, y: Double) =
		run { state.path.rCubicTo(cx1, cy1, cx2, cy2, x, y) }

	fun rect(x: Double, y: Double, width: Double, height: Double) = run { state.path.rect(x, y, width, height) }
	inline fun rectHole(x: Number, y: Number, width: Number, height: Number) =
		run { state.path.rectHole(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble()) }

	fun roundRect(x: Double, y: Double, w: Double, h: Double, rx: Double, ry: Double = rx) =
		run { this.beginPath(); state.path.roundRect(x, y, w, h, rx, ry); this.closePath() }

	fun path(path: GraphicsPath) = run { this.state.path.write(path) }
	fun draw(d: Drawable) = run { d.draw(this) }

	fun strokeRect(x: Double, y: Double, width: Double, height: Double) =
		run { beginPath(); rect(x, y, width, height); stroke() }

	fun fillRect(x: Double, y: Double, width: Double, height: Double) =
		run { beginPath(); rect(x, y, width, height); fill() }

	fun beginPath() = run { state.path = GraphicsPath() }

	fun getBounds(out: Rectangle = Rectangle()) = state.path.getBounds(out)

	fun closePath() = run { state.path.close() }
	fun stroke() = run { if (state.strokeStyle != None) renderer.render(state, fill = false) }
	fun fill() = run { if (state.fillStyle != None) renderer.render(state, fill = true) }

	fun fill(paint: Paint) {
		this.fillStyle(paint) {
			this.fill()
		}
	}

	fun stroke(paint: Paint) {
		this.strokeStyle(paint) {
			this.stroke()
		}
	}

	inline fun stroke(paint: Paint, callback: () -> Unit) {
		callback()
		stroke(paint)
	}

	inline fun stroke(color: RGBA, callback: () -> Unit) {
		callback()
		stroke(Color(color))
	}

	fun fillStroke() = run { fill(); stroke() }
	fun clip() = run { state.clip = state.path }

	fun drawShape(
		shape: Shape,
		rasterizerMethod: Context2d.ShapeRasterizerMethod = Context2d.ShapeRasterizerMethod.X4
	) {
		when (rasterizerMethod) {
			Context2d.ShapeRasterizerMethod.NONE -> {
				shape.draw(this)
			}
			Context2d.ShapeRasterizerMethod.X1, Context2d.ShapeRasterizerMethod.X2, Context2d.ShapeRasterizerMethod.X4 -> {
				val scale = rasterizerMethod.scale
				val newBi = NativeImage(ceil(renderer.width * scale).toInt(), ceil(renderer.height * scale).toInt())
				val bi = newBi.getContext2d(antialiasing = false)
				//val bi = Context2d(AwtContext2dRender(newBi, antialiasing = true))
				//val oldLineScale = bi.lineScale
				//try {
				bi.scale(scale, scale)
				bi.transform(state.transform)
				bi.draw(shape)
				val renderBi = when (rasterizerMethod) {
					Context2d.ShapeRasterizerMethod.X1 -> newBi
					Context2d.ShapeRasterizerMethod.X2 -> newBi.mipmap(1)
					Context2d.ShapeRasterizerMethod.X4 -> newBi.mipmap(2)
					else -> newBi
				}
				keepTransform {
					setTransform(Matrix2d())
					this.renderer.drawImage(renderBi, 0, 0)
				}
				//} finally {
				//	bi.lineScale = oldLineScale
				//}
			}
		}
	}

	fun createLinearGradient(x0: Double, y0: Double, x1: Double, y1: Double) =
		Gradient(Gradient.Kind.LINEAR, x0, y0, 0.0, x1, y1, 0.0)

	fun createRadialGradient(x0: Double, y0: Double, r0: Double, x1: Double, y1: Double, r1: Double) =
		Gradient(Gradient.Kind.RADIAL, x0, y0, r0, x1, y1, r1)

	fun createColor(color: RGBA) = Color(color)
	fun createPattern(
		bitmap: Bitmap,
		repeat: Boolean = false,
		smooth: Boolean = true,
		transform: Matrix2d = Matrix2d()
	) = BitmapPaint(bitmap, transform, repeat, smooth)

	val none = None

	data class Font(val name: String, val size: Double)
	data class TextMetrics(val bounds: Rectangle = Rectangle())

	fun getTextBounds(text: String, out: TextMetrics = TextMetrics()): TextMetrics =
		out.apply { renderer.getBounds(font, text, out) }

	@Suppress("NOTHING_TO_INLINE") // Number inlining
	inline fun fillText(text: String, x: Number, y: Number): Unit =
		renderText(text, x.toDouble(), y.toDouble(), fill = true)

	@Suppress("NOTHING_TO_INLINE") // Number inlining
	inline fun strokeText(text: String, x: Number, y: Number): Unit =
		renderText(text, x.toDouble(), y.toDouble(), fill = false)

	@Suppress("NOTHING_TO_INLINE") // Number inlining
	inline fun fillText(
		text: String,
		x: Number,
		y: Number,
		font: Font? = null,
		halign: HorizontalAlign? = null,
		valign: VerticalAlign? = null,
		color: RGBA? = null
	): Unit {
		font(font, halign, valign) {
			fillStyle(color?.let { createColor(it) } ?: fillStyle) {
				renderText(text, x.toDouble(), y.toDouble(), fill = true)
			}
		}
	}

	fun renderText(text: String, x: Double, y: Double, fill: Boolean): Unit =
		run { renderer.renderText(state, font, text, x, y, fill) }

	fun drawImage(image: Bitmap, x: Int, y: Int, width: Int = image.width, height: Int = image.height) {
		renderer.drawImage(image, x, y, width, height, state.transform)
	}

	interface Paint

	object None : Paint

	data class Color(val color: RGBA) : Paint

	interface TransformedPaint : Paint {
		val transform: Matrix2d
	}

	data class Gradient(
		val kind: Kind,
		val x0: Double,
		val y0: Double,
		val r0: Double,
		val x1: Double,
		val y1: Double,
		val r1: Double,
		val stops: DoubleArrayList = DoubleArrayList(),
		val colors: IntArrayList = IntArrayList(),
		val cycle: CycleMethod = CycleMethod.NO_CYCLE,
		override val transform: Matrix2d = Matrix2d(),
		val interpolationMethod: InterpolationMethod = InterpolationMethod.NORMAL,
		val units: Units = Units.OBJECT_BOUNDING_BOX
	) : TransformedPaint {
		enum class Kind {
			LINEAR, RADIAL
		}

		enum class Units {
			USER_SPACE_ON_USE, OBJECT_BOUNDING_BOX
		}

		enum class InterpolationMethod {
			LINEAR, NORMAL
		}

		val numberOfStops = stops.size

		fun addColorStop(stop: Double, color: Int): Gradient {
			stops += stop
			colors += color
			return this
		}

		fun applyMatrix(m: Matrix2d): Gradient = Gradient(
			kind,
			m.transformX(x0, y0),
			m.transformY(x0, y0),
			r0,
			m.transformX(x1, y1),
			m.transformY(x1, y1),
			r1,
			DoubleArrayList(stops),
			IntArrayList(colors),
			cycle,
			Matrix2d(),
			interpolationMethod,
			units
		)

		override fun toString(): String = when (kind) {
			Kind.LINEAR -> "LinearGradient($x0, $y0, $x1, $y1, $stops, $colors)"
			Kind.RADIAL -> "RadialGradient($x0, $y0, $r0, $x1, $y1, $r1, $stops, $colors)"
		}
	}

	class BitmapPaint(
		val bitmap: Bitmap,
		override val transform: Matrix2d,
		val repeat: Boolean = false,
		val smooth: Boolean = true
	) : TransformedPaint

	interface Drawable {
		fun draw(c: Context2d)
	}

	interface BoundsDrawable : Drawable {
		val bounds: Rectangle
	}

	interface SizedDrawable : Drawable {
		val width: Int
		val height: Int
	}

	class FuncDrawable(val action: Context2d.() -> Unit) : Context2d.Drawable {
		override fun draw(c: Context2d) {
			c.keep {
				action(c)
			}
		}
	}
}

fun Context2d.Drawable.renderTo(ctx: Context2d) = ctx.draw(this)

fun Context2d.SizedDrawable.filled(paint: Context2d.Paint): Context2d.SizedDrawable {
	return object : Context2d.SizedDrawable by this {
		override fun draw(c: Context2d) {
			c.fillStyle = paint
			this@filled.draw(c)
			c.fill()
		}
	}
}

fun Context2d.SizedDrawable.scaled(sx: Number = 1.0, sy: Number = sx): Context2d.SizedDrawable {
	return object : Context2d.SizedDrawable by this {
		override val width: Int = abs(this@scaled.width.toDouble() * sx.toDouble()).toInt()
		override val height: Int = abs(this@scaled.height.toDouble() * sy.toDouble()).toInt()

		override fun draw(c: Context2d) {
			c.scale(sx.toDouble(), sy.toDouble())
			this@scaled.draw(c)
		}
	}
}

fun Context2d.SizedDrawable.translated(tx: Number = 0.0, ty: Number = tx): Context2d.SizedDrawable {
	return object : Context2d.SizedDrawable by this {
		override fun draw(c: Context2d) {
			c.translate(tx.toDouble(), ty.toDouble())
			this@translated.draw(c)
		}
	}
}

fun Context2d.SizedDrawable.render(): NativeImage {
	val image = NativeImage(this.width, this.height)
	val ctx = image.getContext2d()
	this.draw(ctx)
	return image
}

fun Context2d.SizedDrawable.renderNoNative(): Bitmap32 {
	val image = Bitmap32(this.width, this.height)
	val ctx = image.getContext2d()
	this.draw(ctx)
	return image
}

fun Context2d.Drawable.renderToImage(width: Int, height: Int): NativeImage {
	val image = NativeImage(width, height)
	val ctx = image.getContext2d()
	this.draw(ctx)
	return image
}
