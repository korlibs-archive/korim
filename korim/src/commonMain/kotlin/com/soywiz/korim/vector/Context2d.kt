package com.soywiz.korim.vector

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*

class Context2d(val renderer: Renderer) : Disposable, VectorBuilder {
    val width: Int get() = renderer.width
	val height: Int get() = renderer.height

	enum class LineCap { BUTT, ROUND, SQUARE }
	enum class LineJoin { BEVEL, MITER, ROUND }
	enum class CycleMethod { NO_CYCLE, REFLECT, REPEAT }

	enum class ShapeRasterizerMethod(val scale: Float) {
		NONE(0f), X1(1f), X2(2f), X4(4f)
	}

	override fun dispose() {
		renderer.dispose()
	}

	fun withScaledRenderer(scaleX: Float, scaleY: Float = scaleX): Context2d = if (scaleX == 1f && scaleY == 1f) this else Context2d(ScaledRenderer(renderer, scaleX, scaleY))

	class ScaledRenderer(val parent: Renderer, val scaleX: Float, val scaleY: Float) : Renderer() {
		override val width: Int get() = (parent.width / scaleX).toInt()
		override val height: Int get() = (parent.height / scaleY).toInt()

		private inline fun <T> adjustMatrix(transform: Matrix, callback: () -> T): T {
			return transform.keep {
				transform.scale(scaleX, scaleY)
				callback()
			}
		}

		private inline fun <T> adjustState(state: State, callback: () -> T): T =
			adjustMatrix(state.transform) { callback() }

		override fun render(state: State, fill: Boolean): Unit = adjustState(state) { parent.render(state, fill) }
		override fun renderText(state: State, font: Font, text: String, x: Float, y: Float, fill: Boolean): Unit =
			adjustState(state) { parent.renderText(state, font, text, x, y, fill) }

		override fun getBounds(font: Font, text: String, out: TextMetrics): Unit = parent.getBounds(font, text, out)
		override fun drawImage(image: Bitmap, x: Int, y: Int, width: Int, height: Int, transform: Matrix): Unit {
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
		open fun renderText(state: State, font: Font, text: String, x: Float, y: Float, fill: Boolean): Unit = Unit
		open fun getBounds(font: Font, text: String, out: TextMetrics): Unit =
			run { out.bounds.setTo(0.0, 0.0, 0.0, 0.0) }

		open fun drawImage(
			image: Bitmap,
			x: Int,
			y: Int,
			width: Int = image.width,
			height: Int = image.height,
			transform: Matrix = Matrix()
		): Unit {
			val state = State(transform = transform, path = GraphicsPath().apply { rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble()) }, fillStyle = Context2d.BitmapPaint(
				image,
				transform = Matrix()
					.scale(width.toDouble() / image.width.toDouble(), height.toDouble() / image.height.toDouble())
					.translate(x, y)
			))
			render(state, fill = true)
		}

		open fun dispose(): Unit {
		}
	}

	enum class VerticalAlign(val ratio: Float) {
		TOP(0f), MIDDLE(.5f), BASELINE(1f), BOTTOM(1f);

		fun getOffsetY(height: Float, baseline: Float): Float = when (this) {
			BASELINE -> baseline
			else -> -height * ratio
		}
	}

	enum class HorizontalAlign(val ratio: Float) {
		LEFT(0f), CENTER(.5f), RIGHT(1f);

		fun getOffsetX(width: Float): Float = width * ratio
	}

	enum class ScaleMode(val hScale: Boolean, val vScale: Boolean) {
		NONE(false, false), HORIZONTAL(true, false), VERTICAL(false, true), NORMAL(true, true);
	}

	data class State(
		var transform: Matrix = Matrix(),
		var clip: GraphicsPath? = null,
		var path: GraphicsPath = GraphicsPath(),
		var lineScaleMode: ScaleMode = ScaleMode.NORMAL,
		var lineWidth: Float = 1f,
		var lineCap: LineCap = LineCap.BUTT,
		var lineJoin: LineJoin = LineJoin.MITER,
		var miterLimit: Float = 10f,
		var strokeStyle: Paint = Color(Colors.BLACK),
		var fillStyle: Paint = Color(Colors.BLACK),
		var font: Font = Font("sans-serif", 10f),
		var verticalAlign: VerticalAlign = VerticalAlign.BASELINE,
		var horizontalAlign: HorizontalAlign = HorizontalAlign.LEFT,
		var globalAlpha: Float = 1f
	) {
		fun clone(): State = this.copy(
			transform = transform.clone(),
			clip = clip?.clone(),
			path = path.clone()
		)
	}

	var state = State()
	private val stack = Stack<State>()

	var lineScaleMode: ScaleMode by redirectField { state::lineScaleMode }
	var lineWidth: Float by redirectField { state::lineWidth }
	var lineCap: LineCap by redirectField { state::lineCap }
	var strokeStyle: Paint by redirectField { state::strokeStyle }
	var fillStyle: Paint by redirectField { state::fillStyle }
	var font: Font by redirectField { state::font }
	var verticalAlign: VerticalAlign by redirectField { state::verticalAlign }
	var horizontalAlign: HorizontalAlign by redirectField { state::horizontalAlign }
	var globalAlpha: Float by redirectField { state::globalAlpha }

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

	fun save() = run { stack.push(state.clone()) }
	fun restore() = run { state = stack.pop() }

	fun scale(sx: Float, sy: Float = sx) = run { state.transform.prescale(sx, sy) }
	fun rotate(angle: Float) = run { state.transform.prerotate(angle) }
	fun rotateDeg(degs: Float) = run { state.transform.prerotate(Angle.degreesToRadians(degs)) }

	fun translate(tx: Float, ty: Float) = run { state.transform.pretranslate(tx, ty) }
	fun transform(m: Matrix) = run { state.transform.premultiply(m) }
	fun transform(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float) =
		run { state.transform.premultiply(a, b, c, d, tx, ty) }

	fun setTransform(m: Matrix) = run { state.transform.copyFrom(m) }
	fun setTransform(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float) =
		run { state.transform.setTo(a, b, c, d, tx, ty) }

	fun shear(sx: Float, sy: Float) = transform(1f, sy, sx, 1f, 0f, 0f)

    inline fun scale(sx: Number, sy: Number = sx) = scale(sx.toFloat(), sy.toFloat())
    inline fun translate(tx: Number, ty: Number) = translate(tx.toFloat(), ty.toFloat())
    inline fun rotate(angle: Number) = rotate(angle.toFloat())
    inline fun rotateDeg(degs: Number) = rotateDeg(degs.toFloat())
    inline fun shear(sx: Number, sy: Number) = shear(sx.toFloat(), sy.toFloat())

    override val lastX: Float get() = state.path.lastX
    override val lastY: Float get() = state.path.lastY
    override val totalPoints: Int get() = state.path.totalPoints
    override fun close() = state.path.close()
    override fun moveTo(x: Float, y: Float) = run { state.path.moveTo(x, y) }
    override fun lineTo(x: Float, y: Float) = run { state.path.lineTo(x, y) }
    override fun quadTo(controlX: Float, controlY: Float, anchorX: Float, anchorY: Float) = state.path.quadTo(controlX, controlY, anchorX, anchorY)
    override fun cubicTo(cx1: Float, cy1: Float, cx2: Float, cy2: Float, ax: Float, ay: Float) = state.path.cubicTo(cx1, cy1, cx2, cy2, ax, ay)

    fun closePath() = close()

    /*
    fun arc(x: Float, y: Float, r: Float, start: Float, end: Float) = run { state.path.arc(x, y, r, start, end) }
    fun arcTo(x1: Float, y1: Float, x2: Float, y2: Float, r: Float) = run { state.path.arcTo(x1, y1, x2, y2, r) }
    fun rMoveTo(x: Float, y: Float) = run { state.path.rMoveTo(x, y) }
    fun moveToH(x: Float) = run { state.path.moveToH(x) }
    fun moveToV(y: Float) = run { state.path.moveToV(y) }
    fun rMoveToH(x: Float) = run { state.path.rMoveToH(x) }
    fun rMoveToV(y: Float) = run { state.path.rMoveToV(y) }
    fun lineToH(x: Float) = run { state.path.lineToH(x) }
    fun lineToV(y: Float) = run { state.path.lineToV(y) }
    fun rLineToH(x: Float) = run { state.path.rLineToH(x) }
    fun rLineToV(y: Float) = run { state.path.rLineToV(y) }
    fun rLineTo(x: Float, y: Float) = run { state.path.rLineTo(x, y) }
    fun circle(x: Float, y: Float, radius: Float) = arc(x, y, radius, 0.0, PI * 2.0)
    fun quadraticCurveTo(cx: Float, cy: Float, ax: Float, ay: Float) = run { state.path.quadTo(cx, cy, ax, ay) }
    fun rQuadraticCurveTo(cx: Float, cy: Float, ax: Float, ay: Float) = run { state.path.rQuadTo(cx, cy, ax, ay) }
    fun bezierCurveTo(cx1: Float, cy1: Float, cx2: Float, cy2: Float, x: Float, y: Float) = cubicTo(cx1, cy1, cx2, cy2, x, y)
    fun rBezierCurveTo(cx1: Float, cy1: Float, cx2: Float, cy2: Float, x: Float, y: Float) =
        run { state.path.rCubicTo(cx1, cy1, cx2, cy2, x, y) }
    fun rect(x: Float, y: Float, width: Float, height: Float) = run { state.path.rect(x, y, width, height) }
    inline fun rectHole(x: Number, y: Number, width: Number, height: Number) =
        run { state.path.rectHole(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble()) }
    fun roundRect(x: Float, y: Float, w: Float, h: Float, rx: Float, ry: Float = rx) =
        run { this.beginPath(); state.path.roundRect(x, y, w, h, rx, ry); this.closePath() }
    inline fun moveTo(x: Number, y: Number) = moveTo(x.toDouble(), y.toDouble())
    inline fun lineTo(x: Number, y: Number) = lineTo(x.toDouble(), y.toDouble())
    inline fun quadraticCurveTo(cx: Number, cy: Number, ax: Number, ay: Number) =
        quadraticCurveTo(cx.toDouble(), cy.toDouble(), ax.toDouble(), ay.toDouble())

    inline fun bezierCurveTo(cx1: Number, cy1: Number, cx2: Number, cy2: Number, ax: Number, ay: Number) =
        bezierCurveTo(cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())

    inline fun arcTo(x1: Number, y1: Number, x2: Number, y2: Number, radius: Number) =
        arcTo(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(), radius.toDouble())

    fun moveTo(p: Point) = moveTo(p.x, p.y)
    fun lineTo(p: Point) = lineTo(p.x, p.y)
    fun quadraticCurveTo(c: Point, a: Point) = quadraticCurveTo(c.x, c.y, a.x, a.y)
    fun bezierCurveTo(c1: Point, c2: Point, a: Point) = bezierCurveTo(c1.x, c1.y, c2.x, c2.y, a.x, a.y)
    fun arcTo(p1: Point, p2: Point, radius: Float) = arcTo(p1.x, p1.y, p2.x, p2.y, radius)

    inline fun rect(x: Number, y: Number, width: Number, height: Number) =
        rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
    */

    inline fun strokeRect(x: Number, y: Number, width: Number, height: Number) =
        strokeRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

    inline fun fillRect(x: Number, y: Number, width: Number, height: Number) =
        fillRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

    inline fun fillRoundRect(x: Number, y: Number, width: Number, height: Number, rx: Number, ry: Number = rx) {
        beginPath()
        roundRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(), rx.toDouble(), ry.toDouble())
        fill()
    }
    fun strokeDot(x: Float, y: Float) = run { beginPath(); moveTo(x, y); lineTo(x, y); stroke() }

	fun path(path: GraphicsPath) = run { this.state.path.write(path) }
	fun draw(d: Drawable) = run { d.draw(this) }

	fun strokeRect(x: Float, y: Float, width: Float, height: Float) =
		run { beginPath(); rect(x, y, width, height); stroke() }

	fun fillRect(x: Float, y: Float, width: Float, height: Float) =
		run { beginPath(); rect(x, y, width, height); fill() }

	fun beginPath() = run { state.path = GraphicsPath() }

	fun getBounds(out: Rectangle = Rectangle()) = state.path.getBounds(out)

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
					setTransform(Matrix())
					this.renderer.drawImage(renderBi, 0, 0)
				}
				//} finally {
				//	bi.lineScale = oldLineScale
				//}
			}
		}
	}

	fun createLinearGradient(x0: Float, y0: Float, x1: Float, y1: Float) =
		Gradient(Gradient.Kind.LINEAR, x0, y0, 0f, x1, y1, 0f)

	fun createRadialGradient(x0: Float, y0: Float, r0: Float, x1: Float, y1: Float, r1: Float) =
		Gradient(Gradient.Kind.RADIAL, x0, y0, r0, x1, y1, r1)

	fun createColor(color: RGBA) = Color(color)
	fun createPattern(
		bitmap: Bitmap,
		repeat: Boolean = false,
		smooth: Boolean = true,
		transform: Matrix = Matrix()
	) = BitmapPaint(bitmap, transform, repeat, smooth)

	val none = None

	data class Font(val name: String, val size: Float)
	data class TextMetrics(val bounds: Rectangle = Rectangle())

	fun getTextBounds(text: String, out: TextMetrics = TextMetrics()): TextMetrics =
		out.apply { renderer.getBounds(font, text, out) }

	@Suppress("NOTHING_TO_INLINE") // Number inlining
	inline fun fillText(text: String, x: Number, y: Number): Unit =
		renderText(text, x.toFloat(), y.toFloat(), fill = true)

	@Suppress("NOTHING_TO_INLINE") // Number inlining
	inline fun strokeText(text: String, x: Number, y: Number): Unit =
		renderText(text, x.toFloat(), y.toFloat(), fill = false)

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
				renderText(text, x.toFloat(), y.toFloat(), fill = true)
			}
		}
	}

	fun renderText(text: String, x: Float, y: Float, fill: Boolean): Unit =
		run { renderer.renderText(state, font, text, x, y, fill) }

	fun drawImage(image: Bitmap, x: Int, y: Int, width: Int = image.width, height: Int = image.height) {
		renderer.drawImage(image, x, y, width, height, state.transform)
	}

	interface Paint

	object None : Paint

	data class Color(val color: RGBA) : Paint

	interface TransformedPaint : Paint {
		val transform: Matrix
	}

	data class Gradient(
        val kind: Kind,
        val x0: Float,
        val y0: Float,
        val r0: Float,
        val x1: Float,
        val y1: Float,
        val r1: Float,
        val stops: FloatArrayList = FloatArrayList(),
        val colors: IntArrayList = IntArrayList(),
        val cycle: CycleMethod = CycleMethod.NO_CYCLE,
        override val transform: Matrix = Matrix(),
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

		fun addColorStop(stop: Float, color: Int): Gradient {
			stops += stop
			colors += color
			return this
		}

		fun applyMatrix(m: Matrix): Gradient = Gradient(
			kind,
			m.transformX(x0, y0),
			m.transformY(x0, y0),
			r0,
			m.transformX(x1, y1),
			m.transformY(x1, y1),
			r1,
			FloatArrayList(stops),
			IntArrayList(colors),
			cycle,
			Matrix(),
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
		override val transform: Matrix,
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
