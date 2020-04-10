package com.soywiz.korim.vector

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.FontRegistry
import com.soywiz.korim.font.SystemFont
import com.soywiz.korim.font.SystemFontRegistry
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*

open class Context2d constructor(val renderer: Renderer) : Disposable, VectorBuilder {
    var debug: Boolean
        get() = renderer.debug
        set(value) = run { renderer.debug = value }

    protected open val rendererWidth get() = renderer.width
    protected open val rendererHeight get() = renderer.height
    protected open fun rendererRender(state: State, fill: Boolean) = renderer.render(state, fill)
    protected open fun rendererDrawImage(image: Bitmap, x: Double, y: Double, width: Double = image.width.toDouble(), height: Double = image.height.toDouble(), transform: Matrix = Matrix()) = renderer.drawImage(image, x, y, width, height, transform)
    protected open fun rendererDispose() = renderer.dispose()
    protected open fun rendererBufferingStart() = renderer.bufferingStart()
    protected open fun rendererBufferingEnd() = renderer.bufferingEnd()
    open fun Renderer.rendererRenderSystemText(state: State, font: SystemFont, fontSize: Double, text: String, x: Double, y: Double, fill: Boolean) =
        renderer.renderText(state, font, fontSize, text, x, y, fill)
    protected open fun rendererSystemGetBounds(font: SystemFont, fontSize: Double, text: String, out: TextMetrics): Unit = renderer.getBounds(font, fontSize, text, out)

    open val width: Int get() = rendererWidth
    open val height: Int get() = rendererHeight

	override fun dispose() {
		rendererDispose()
	}

    fun withScaledRenderer(scaleX: Double, scaleY: Double = scaleX): Context2d = if (scaleX == 1.0 && scaleY == 1.0) this else Context2d(ScaledRenderer(renderer, scaleX, scaleY))

	class ScaledRenderer(val parent: Renderer, val scaleX: Double, val scaleY: Double) : Renderer() {
		override val width: Int get() = (parent.width / scaleX).toInt()
		override val height: Int get() = (parent.height / scaleY).toInt()

		private inline fun <T> adjustMatrix(transform: Matrix, callback: () -> T): T = transform.keep {
            transform.scale(scaleX, scaleY)
            callback()
        }

		private inline fun <T> adjustState(state: State, callback: () -> T): T =
			adjustMatrix(state.transform) { callback() }

		override fun render(state: State, fill: Boolean): Unit = adjustState(state) { parent.render(state, fill) }
		override fun renderText(state: State, font: Font, fontSize: Double, text: String, x: Double, y: Double, fill: Boolean): Unit =
			adjustState(state) { parent.renderText(state, font, fontSize, text, x, y, fill) }

		override fun getBounds(font: Font, fontSize: Double, text: String, out: TextMetrics): Unit = parent.getBounds(font, fontSize, text, out)
		override fun drawImage(image: Bitmap, x: Double, y: Double, width: Double, height: Double, transform: Matrix): Unit {
			adjustMatrix(transform) { parent.drawImage(image, x, y, width, height, transform) }
		}
	}

    inline fun <T> buffering(callback: () -> T): T {
        rendererBufferingStart()
        try {
            return callback()
        } finally {
            rendererBufferingEnd()
        }
    }

    abstract class BufferedRenderer : Renderer() {
        abstract fun flushCommands()

        data class RenderCommand(
            val state: State,
            val fill: Boolean,
            val font: Font? = null,
            val fontSize: Double = 0.0,
            val text: String? = null,
            val x: Double = 0.0,
            val y: Double = 0.0
        )
        protected val commands = arrayListOf<RenderCommand>()

        final override fun render(state: State, fill: Boolean) {
            commands += RenderCommand(state.clone(), fill)
            if (!isBuffering()) flush()
        }

        final override fun renderText(state: State, font: Font, fontSize: Double, text: String, x: Double, y: Double, fill: Boolean) {
            commands += RenderCommand(state.clone(), fill, font, fontSize, text, x, y)
            if (!isBuffering()) flush()
        }

        final override fun flush() = flushCommands()
    }

	abstract class Renderer {
		companion object {
			val DUMMY = object : Renderer() {
				override val width: Int = 128
				override val height: Int = 128
			}
		}

        var debug: Boolean = false
		abstract val width: Int
		abstract val height: Int

        inline fun <T> buffering(callback: () -> T): T {
            bufferingStart()
            try {
                return callback()
            } finally {
                bufferingEnd()
            }
        }

        private var bufferingLevel = 0
        protected fun isBuffering() = bufferingLevel > 0
        open protected fun flush() = Unit
        fun bufferingStart() = bufferingLevel++
        fun bufferingEnd() {
            bufferingLevel--
            if (bufferingLevel == 0) {
                flush()
            }
        }
		open fun render(state: State, fill: Boolean): Unit = Unit
		open fun renderText(state: State, font: Font, fontSize: Double, text: String, x: Double, y: Double, fill: Boolean): Unit = Unit
		open fun getBounds(font: Font, fontSize: Double, text: String, out: TextMetrics): Unit =
			run { out.bounds.setTo(0.0, 0.0, 0.0, 0.0) }

		open fun drawImage(
			image: Bitmap,
			x: Double,
			y: Double,
			width: Double = image.width.toDouble(),
			height: Double = image.height.toDouble(),
			transform: Matrix = Matrix()
		) {
			render(State(
                transform = transform,
                path = GraphicsPath().apply { rect(x, y, width, height) },
                fillStyle = BitmapPaint(image,
                    transform = Matrix()
                        .scale(width / image.width.toDouble(), height / image.height.toDouble())
                        .translate(x, y)
                )
            ), fill = true)
		}

        inline fun drawImage(
            image: Bitmap,
            x: Number, y: Number, width: Number = image.width, height: Number = image.height,
            transform: Matrix = Matrix()
        ) = drawImage(image, x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(), transform)

		open fun dispose(): Unit {
            flush()
		}
	}

	data class State constructor(
        var transform: Matrix = Matrix(),
        var clip: GraphicsPath? = null,
        var path: GraphicsPath = GraphicsPath(),
        var lineScaleMode: LineScaleMode = LineScaleMode.NORMAL,
        var lineWidth: Double = 1.0,
        var startLineCap: LineCap = LineCap.BUTT,
        var endLineCap: LineCap = LineCap.BUTT,
        var lineJoin: LineJoin = LineJoin.MITER,
        var miterLimit: Double = 4.0,
        var strokeStyle: Paint = DefaultPaint,
        var fillStyle: Paint = DefaultPaint,
        var fontRegistry: FontRegistry = SystemFontRegistry,
        var font: Font = SystemFontRegistry.DEFAULT_FONT,
        var fontSize: Double = 24.0,
        var verticalAlign: VerticalAlign = VerticalAlign.BASELINE,
        var horizontalAlign: HorizontalAlign = HorizontalAlign.LEFT,
        var globalAlpha: Double = 1.0
	) {
        var lineCap: LineCap
            get() = startLineCap
            set(value) {
                startLineCap = value
                endLineCap = value
            }

        fun clone(): State = this.copy(
			transform = transform.clone(),
			clip = clip?.clone(),
			path = path.clone()
		)
	}

	var state = State()
	private val stack = Stack<State>()

	var lineScaleMode: LineScaleMode by { state::lineScaleMode }.redirected()
	var lineWidth: Double by { state::lineWidth }.redirected()
	var lineCap: LineCap by { state::lineCap }.redirected()
    var startLineCap: LineCap by { state::startLineCap }.redirected()
    var endLineCap: LineCap by { state::startLineCap }.redirected()
    var lineJoin: LineJoin by { state::lineJoin }.redirected()
	var strokeStyle: Paint by { state::strokeStyle }.redirected()
	var fillStyle: Paint by { state::fillStyle }.redirected()
    var fontRegistry: FontRegistry by { state::fontRegistry }.redirected()
	var font: Font by { state::font }.redirected()
    var fontName: String
        get() = font.name
        set(value) = run { font = fontRegistry[value] }
    var fontSize: Double by { state::fontSize }.redirected()
	var verticalAlign: VerticalAlign by { state::verticalAlign }.redirected()
	var horizontalAlign: HorizontalAlign by { state::horizontalAlign }.redirected()
	var globalAlpha: Double by { state::globalAlpha }.redirected()

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

	inline fun font(
        font: Font = this.font,
        halign: HorizontalAlign = this.horizontalAlign,
        valign: VerticalAlign = this.verticalAlign,
        fontSize: Double = this.fontSize,
        callback: () -> Unit
    ) {
		val oldFont = this.font
        val oldFontSize = this.fontSize
		val oldHalign = this.horizontalAlign
		val oldValign = this.verticalAlign
		try {
            this.font = font
            this.fontSize = fontSize
            this.horizontalAlign = halign
            this.verticalAlign = valign
			callback()
		} finally {
			this.font = oldFont
            this.fontSize = oldFontSize
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

	inline fun scale(sx: Number, sy: Number = sx) = scale(sx.toDouble(), sy.toDouble())
	inline fun translate(tx: Number, ty: Number) = translate(tx.toDouble(), ty.toDouble())
	inline fun rotate(angle: Number) = rotate(angle.toDouble())
	inline fun rotateDeg(degs: Number) = rotateDeg(degs.toDouble())

	fun scale(sx: Double, sy: Double = sx) = run { state.transform.prescale(sx, sy) }
    fun rotate(angle: Angle) = run { state.transform.prerotate(angle) }
	fun rotate(angle: Double) = run { state.transform.prerotate(angle.radians) }
	fun rotateDeg(degs: Double) = run { state.transform.prerotate(degs.degrees) }

	fun translate(tx: Double, ty: Double) = run { state.transform.pretranslate(tx, ty) }
	fun transform(m: Matrix) = run { state.transform.premultiply(m) }
	fun transform(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double) =
		run { state.transform.premultiply(a, b, c, d, tx, ty) }

	fun setTransform(m: Matrix) = run { state.transform.copyFrom(m) }
	fun setTransform(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double) =
		run { state.transform.setTo(a, b, c, d, tx, ty) }

	fun shear(sx: Double, sy: Double) = transform(1.0, sy, sx, 1.0, 0.0, 0.0)

    override val lastX: Double get() = state.path.lastX
    override val lastY: Double get() = state.path.lastY
    override val totalPoints: Int  get() = state.path.totalPoints

    override fun close() = state.path.close()

    //private fun transX(x: Double, y: Double) = state.transform.transformX(x, y)
    //private fun transY(x: Double, y: Double) = state.transform.transformY(x, y)

    private fun transX(x: Double, y: Double) = x
    private fun transY(x: Double, y: Double) = y

    override fun moveTo(x: Double, y: Double) = state.path.moveTo(transX(x, y), transY(x, y))
    override fun lineTo(x: Double, y: Double) = state.path.lineTo(transX(x, y), transY(x, y))
    override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) = state.path.quadTo(
        transX(cx, cy), transY(cx, cy),
        transX(ax, ay), transY(ax, ay)
    )
    override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) =
        state.path.cubicTo(
            transX(cx1, cy1), transY(cx1, cy1),
            transX(cx2, cy2), transY(cx2, cy2),
            transX(ax, ay), transY(ax, ay)
        )

	inline fun strokeRect(x: Number, y: Number, width: Number, height: Number) =
		strokeRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

	inline fun fillRect(x: Number, y: Number, width: Number, height: Number) =
		fillRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

	inline fun fillRoundRect(x: Number, y: Number, width: Number, height: Number, rx: Number, ry: Number = rx) {
		beginPath()
		roundRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(), rx.toDouble(), ry.toDouble())
		fill()
	}

	fun strokeDot(x: Double, y: Double) = run { beginPath(); moveTo(x, y); lineTo(x, y); stroke() }

	fun path(path: GraphicsPath) = run { this.state.path.write(path) }
	fun draw(d: Drawable) = run { d.draw(this) }

	fun strokeRect(x: Double, y: Double, width: Double, height: Double) =
		run { beginPath(); rect(x, y, width, height); stroke() }

	fun fillRect(x: Double, y: Double, width: Double, height: Double) =
		run { beginPath(); rect(x, y, width, height); fill() }

	fun beginPath() = run { state.path = GraphicsPath() }

	fun getBounds(out: Rectangle = Rectangle()) = state.path.getBounds(out)

	fun stroke() = run { if (state.strokeStyle != None) rendererRender(state, fill = false) }
    fun fill() = run { if (state.fillStyle != None) rendererRender(state, fill = true) }

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

    inline fun fill(color: RGBA, block: () -> Unit) {
        block()
        fill(Color(color))
    }

    inline fun fill(paint: Paint, block: () -> Unit) {
        block()
        fill(paint)
    }

    inline fun stroke(paint: Paint, lineWidth: Double = this.lineWidth, lineCap: LineCap = this.lineCap, lineJoin: LineJoin = this.lineJoin, callback: () -> Unit) {
		callback()
        this.lineWidth = lineWidth
        this.lineCap = lineCap
        this.lineJoin = lineJoin
		stroke(paint)
	}

	inline fun stroke(color: RGBA, lineWidth: Double = this.lineWidth, lineCap: LineCap = this.lineCap, lineJoin: LineJoin = this.lineJoin, callback: () -> Unit) {
		callback()
        this.lineWidth = lineWidth
        this.lineCap = lineCap
        this.lineJoin = lineJoin
		stroke(Color(color))
	}

    inline fun fillStroke(fill: Paint, stroke: Paint, callback: () -> Unit) {
        callback()
        fill(fill)
        stroke(stroke)
    }

    fun fillStroke() = run { fill(); stroke() }
	fun clip() = run { state.clip = state.path }

    fun drawShape(
		shape: Shape,
		rasterizerMethod: ShapeRasterizerMethod = ShapeRasterizerMethod.X4
	) {
		when (rasterizerMethod) {
			ShapeRasterizerMethod.NONE -> {
				shape.draw(this)
			}
			ShapeRasterizerMethod.X1, ShapeRasterizerMethod.X2, ShapeRasterizerMethod.X4 -> {
				val scale = rasterizerMethod.scale
				val newBi = NativeImage(ceil(rendererWidth * scale).toInt(), ceil(rendererHeight * scale).toInt())
				val bi = newBi.getContext2d(antialiasing = false)
				//val bi = Context2d(AwtContext2dRender(newBi, antialiasing = true))
				//val oldLineScale = bi.lineScale
				//try {
				bi.scale(scale, scale)
				bi.transform(state.transform)
				bi.draw(shape)
				val renderBi = when (rasterizerMethod) {
					ShapeRasterizerMethod.X1 -> newBi
					ShapeRasterizerMethod.X2 -> newBi.mipmap(1)
					ShapeRasterizerMethod.X4 -> newBi.mipmap(2)
					else -> newBi
				}
				keepTransform {
					setTransform(Matrix())
					this.rendererDrawImage(renderBi, 0.0, 0.0)
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

    inline fun createLinearGradient(x0: Number, y0: Number, x1: Number, y1: Number, block: Gradient.() -> Unit = {}) =
        Gradient(Gradient.Kind.LINEAR, x0.toDouble(), y0.toDouble(), 0.0, x1.toDouble(), y1.toDouble(), 0.0).also(block)
    inline fun createRadialGradient(x0: Number, y0: Number, r0: Number, x1: Number, y1: Number, r1: Number, block: Gradient.() -> Unit = {}) =
        Gradient(Gradient.Kind.RADIAL, x0.toDouble(), y0.toDouble(), r0.toDouble(), x1.toDouble(), y1.toDouble(), r1.toDouble()).also(block)

    fun createColor(color: RGBA) = Color(color)
	fun createPattern(
		bitmap: Bitmap,
		repeat: Boolean = false,
		smooth: Boolean = true,
		transform: Matrix = Matrix()
	) = BitmapPaint(bitmap, transform, repeat, smooth)

	val none = None

	fun getTextBounds(text: String, out: TextMetrics = TextMetrics()): TextMetrics {
        return font.getTextBounds(fontSize, text, out)
    }

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
        font: Font = this.font,
        fontSize: Double = this.fontSize,
        halign: HorizontalAlign = this.horizontalAlign,
        valign: VerticalAlign = this.verticalAlign,
        color: RGBA? = null
	): Unit {
		font(font, halign, valign, fontSize) {
			fillStyle(color?.let { createColor(it) } ?: fillStyle) {
				renderText(text, x.toDouble(), y.toDouble(), fill = true)
			}
		}
	}

    open fun renderText(text: String, x: Double, y: Double, fill: Boolean): Unit {
        font.renderText(this, fontSize, text, x, y, fill)
    }

    // @TODO: Fix this!
    open fun drawImage(image: Bitmap, x: Double, y: Double, width: Double = image.width.toDouble(), height: Double = image.height.toDouble()) =
        rendererDrawImage(image, x, y, width, height, state.transform)

    // @TODO: Fix this!
    inline fun drawImage(image: Bitmap, x: Number, y: Number, width: Number = image.width.toDouble(), height: Number = image.height.toDouble())
        = drawImage(image, x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

    interface Paint

	object None : Paint

	open class Color(val color: RGBA) : Paint

    object DefaultPaint : Color(Colors.BLACK)

    interface TransformedPaint : Paint {
		val transform: Matrix
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

		val numberOfStops get() = stops.size

		fun addColorStop(stop: Double, color: RGBA): Gradient = add(stop, color)

        fun add(stop: Double, color: RGBA): Gradient = this.apply {
            stops += stop
            colors += color.value
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
			DoubleArrayList(stops),
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
	) : TransformedPaint {
        val bmp32 = bitmap.toBMP32()
    }

	interface Drawable {
		fun draw(c: Context2d)
	}

	interface SizedDrawable : Drawable {
		val width: Int
		val height: Int
	}

    interface BoundsDrawable : SizedDrawable {
        val bounds: Rectangle
        val left: Int get() = bounds.left.toInt()
        val top: Int get() = bounds.top.toInt()
        override val width: Int get() = bounds.width.toInt()
        override val height: Int get() = bounds.height.toInt()
    }

    class FuncDrawable(val action: Context2d.() -> Unit) : Context2d.Drawable {
		override fun draw(c: Context2d) {
			c.keep {
				action(c)
			}
		}
	}

    data class StrokeInfo(
        val thickness: Double = 1.0, val pixelHinting: Boolean = false,
        val scaleMode: LineScaleMode = LineScaleMode.NORMAL,
        val startCap: LineCap = LineCap.BUTT,
        val endCap: LineCap = LineCap.BUTT,
        val lineJoin: LineJoin = LineJoin.MITER,
        val miterLimit: Double = 20.0
    )
}

fun RGBA.toFill() = Context2d.Color(this)

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
