package com.soywiz.korim.awt

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.Font
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import java.awt.*
import java.awt.Rectangle
import java.awt.RenderingHints.*
import java.awt.font.*
import java.awt.geom.*
import java.awt.image.*
import java.nio.*

const val AWT_INTERNAL_IMAGE_TYPE_PRE = BufferedImage.TYPE_INT_ARGB_PRE
const val AWT_INTERNAL_IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB

fun BufferedImage.clone(
	width: Int = this.width,
	height: Int = this.height,
	type: Int = AWT_INTERNAL_IMAGE_TYPE_PRE
): BufferedImage {
	val out = BufferedImage(width, height, type)
	//println("BufferedImage.clone:${this.type} -> ${out.type}")
	val g = out.createGraphics(false)
	g.drawImage(this, 0, 0, width, height, null)
	g.dispose()
	return out
}

class AwtNativeImage(val awtImage: BufferedImage) : NativeImage(awtImage.width, awtImage.height, awtImage, premultiplied = (awtImage.type == BufferedImage.TYPE_INT_ARGB_PRE)) {
	override val name: String = "AwtNativeImage"
	override fun toNonNativeBmp(): Bitmap = awtImage.toBMP32()
	override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(AwtContext2dRender(awtImage, antialiasing))

	val dataBuffer = awtImage.raster.dataBuffer

	private val rbuffer: ByteBuffer by lazy {
		ByteBuffer.allocateDirect(width * height * 4).apply {
            (this as Buffer).clear()
			val ib = asIntBuffer()
			when (dataBuffer) {
				// @TODO: Swap Bytes
				is DataBufferByte -> put(dataBuffer.data)
				is DataBufferInt -> ib.put(dataBuffer.data)
				else -> TODO("dataBuffer: $dataBuffer")
			}
			for (n in 0 until area) ib.put(n, argb2rgba(ib.get(n)))
            (this as Buffer).position(width * height * 4)
			//println("BYTES: ${bytes.size}")
			//println("BYTES: ${bytes.size}")
            (this as Buffer).flip()
		}
	}

	private fun argb2rgba(col: Int): Int = (col shl 8) or (col ushr 24)

	val buffer: ByteBuffer get() = rbuffer.apply { (this as Buffer).rewind() }
}

//fun createRenderingHints(antialiasing: Boolean): RenderingHints = RenderingHints(mapOf<RenderingHints.Key, Any>())

fun createRenderingHints(antialiasing: Boolean): RenderingHints = RenderingHints(
	if (antialiasing) {
		mapOf(
			KEY_ANTIALIASING to java.awt.RenderingHints.VALUE_ANTIALIAS_ON
			, RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_QUALITY
			, RenderingHints.KEY_COLOR_RENDERING to RenderingHints.VALUE_COLOR_RENDER_QUALITY
			, RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BILINEAR
			, RenderingHints.KEY_ALPHA_INTERPOLATION to RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
			, RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_ON
			, RenderingHints.KEY_FRACTIONALMETRICS to RenderingHints.VALUE_FRACTIONALMETRICS_ON
		)
	} else {
		mapOf(
			KEY_ANTIALIASING to java.awt.RenderingHints.VALUE_ANTIALIAS_OFF
			, RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_SPEED
			, RenderingHints.KEY_COLOR_RENDERING to RenderingHints.VALUE_COLOR_RENDER_SPEED
			, RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
			, RenderingHints.KEY_ALPHA_INTERPOLATION to RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED
			, RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
			, RenderingHints.KEY_FRACTIONALMETRICS to RenderingHints.VALUE_FRACTIONALMETRICS_OFF
		)
	}
)

fun BufferedImage.createGraphics(antialiasing: Boolean): Graphics2D = this.createGraphics().apply {
	addRenderingHints(createRenderingHints(antialiasing))
}

//private fun BufferedImage.scaled(scale: Double): BufferedImage {
//	val out = BufferedImage(Math.ceil(this.width * scale).toInt(), Math.ceil(this.height * scale).toInt(), this.type)
//	out.createGraphics(antialiasing = true).drawImage(this, 0, 0, out.width, out.height, null)
//	return out
//}

class AwtContext2dRender(val awtImage: BufferedImage, val antialiasing: Boolean = true, val warningProcessor: ((message: String) -> Unit)? = null) : Context2d.Renderer() {
	//val nativeImage = AwtNativeImage(awtImage)
	override val width: Int get() = awtImage.width
	override val height: Int get() = awtImage.height
	val awtTransform = AffineTransform()
	val g = awtImage.createGraphics(antialiasing = antialiasing)

	val hints = createRenderingHints(antialiasing)

	fun GraphicsPath.toJava2dPaths(): List<java.awt.geom.Path2D.Double> {
		if (this.isEmpty()) return listOf()
		val winding =
			if (winding == Winding.EVEN_ODD) java.awt.geom.GeneralPath.WIND_EVEN_ODD else java.awt.geom.GeneralPath.WIND_NON_ZERO
		//val winding = java.awt.geom.GeneralPath.WIND_NON_ZERO
		//val winding = java.awt.geom.GeneralPath.WIND_EVEN_ODD
		val polylines = ArrayList<java.awt.geom.Path2D.Double>()
		var parts = 0
		var polyline = java.awt.geom.Path2D.Double(winding)
		//kotlin.io.println("---")

		fun flush() {
			if (parts > 0) {
				polylines += polyline
				polyline = java.awt.geom.Path2D.Double(winding)
			}
			parts = 0
		}

		this.visitCmds(
			moveTo = { x, y ->
				//flush()
				polyline.moveTo(x, y)
				//kotlin.io.println("moveTo: $x, $y")
			},
			lineTo = { x, y ->
				polyline.lineTo(x, y)
				//kotlin.io.println("lineTo: $x, $y")
				parts++
			},
			quadTo = { cx, cy, ax, ay ->
				polyline.quadTo(cx, cy, ax, ay)
				parts++
			},
			cubicTo = { cx1, cy1, cx2, cy2, ax, ay ->
				polyline.curveTo(cx1, cy1, cx2, cy2, ax, ay)
				parts++
			},
			close = {
				polyline.closePath()
				//kotlin.io.println("closePath")
				parts++
			}
		)
		flush()
		return polylines
	}

	fun GraphicsPath.toJava2dPath(): java.awt.geom.Path2D.Double? {
		return toJava2dPaths().firstOrNull()
	}

	//override fun renderShape(shape: Shape, transform: Matrix, shapeRasterizerMethod: ShapeRasterizerMethod) {
	//	when (shapeRasterizerMethod) {
	//		ShapeRasterizerMethod.NONE -> {
	//			super.renderShape(shape, transform, shapeRasterizerMethod)
	//		}
	//		ShapeRasterizerMethod.X1, ShapeRasterizerMethod.X2, ShapeRasterizerMethod.X4 -> {
	//			val scale = shapeRasterizerMethod.scale
	//			val newBi = BufferedImage(Math.ceil(awtImage.width * scale).toInt(), Math.ceil(awtImage.height * scale).toInt(), awtImage.type)
	//			val bi = Context2d(AwtContext2dRender(newBi, antialiasing = false))
	//			bi.scale(scale, scale)
	//			bi.transform(transform)
	//			bi.draw(shape)
	//			val renderBi = when (shapeRasterizerMethod) {
	//				ShapeRasterizerMethod.X1 -> newBi
	//				ShapeRasterizerMethod.X2 -> newBi.scaled(0.5)
	//				ShapeRasterizerMethod.X4 -> newBi.scaled(0.5).scaled(0.5)
	//				else -> newBi
	//			}
	//			this.g.drawImage(renderBi, 0, 0, null)
	//		}
	//	}
	//}

	override fun drawImage(image: Bitmap, x: Double, y: Double, width: Double, height: Double, transform: Matrix) {
		//transform.toAwt()
		//BufferedImageOp
		this.g.drawImage((image.ensureNative() as AwtNativeImage).awtImage, x.toInt(), y.toInt(), width.toInt(), height.toInt(), null)
	}

	fun convertColor(c: RGBA): java.awt.Color = java.awt.Color(c.r, c.g, c.b, c.a)

	fun CycleMethod.toAwt() = when (this) {
		CycleMethod.NO_CYCLE -> MultipleGradientPaint.CycleMethod.NO_CYCLE
		CycleMethod.REPEAT -> MultipleGradientPaint.CycleMethod.REPEAT
		CycleMethod.REFLECT -> MultipleGradientPaint.CycleMethod.REFLECT
	}

	fun Context2d.Paint.toAwt(transform: AffineTransform): java.awt.Paint = try {
		this.toAwtUnsafe(transform)
	} catch (e: Throwable) {
        warningProcessor?.invoke("Context2d.Paint.toAwt: $e")
		Color.PINK
	}

	//fun Context2d.Paint.toAwt(transform: AffineTransform): java.awt.Paint = this.toAwtUnsafe(transform)

	fun Matrix.toAwt() = AffineTransform(this.a, this.b, this.c, this.d, this.tx, this.ty)

	fun Context2d.Gradient.InterpolationMethod.toAwt() = when (this) {
		Context2d.Gradient.InterpolationMethod.LINEAR -> MultipleGradientPaint.ColorSpaceType.LINEAR_RGB
		Context2d.Gradient.InterpolationMethod.NORMAL -> MultipleGradientPaint.ColorSpaceType.SRGB
	}

	fun Context2d.Paint.toAwtUnsafe(transform: AffineTransform): java.awt.Paint = when (this) {
		is Context2d.Color -> convertColor(this.color)
		is Context2d.TransformedPaint -> {
			val t1 = AffineTransform(this.transform.toAwt())
			//t1.preConcatenate(this.transform.toAwt())
			//t1.preConcatenate(transform)

			when (this) {
				is Context2d.Gradient -> {
					val pairs = this.stops.map(Double::toFloat).zip(this.colors.map { convertColor(RGBA(it)) })
						.distinctBy { it.first }
					val stops = pairs.map { it.first }.toFloatArray()
					val colors = pairs.map { it.second }.toTypedArray()
					val defaultColor = colors.firstOrNull() ?: Color.PINK

					when (this.kind) {
						Context2d.Gradient.Kind.LINEAR -> {
							val valid = (pairs.size >= 2) && ((x0 != x1) || (y0 != y1))
							if (valid) {
								java.awt.LinearGradientPaint(
									Point2D.Double(this.x0, this.y0),
									Point2D.Double(this.x1, this.y1),
									stops,
									colors,
									this.cycle.toAwt(),
									this.interpolationMethod.toAwt(),
									t1
								)
							} else {
								defaultColor
							}
						}
						Context2d.Gradient.Kind.RADIAL -> {
							val valid = (pairs.size >= 2)
							if (valid) {
								java.awt.RadialGradientPaint(
									Point2D.Double(this.x0, this.y0),
									this.r1.toFloat(),
									Point2D.Double(this.x1, this.y1),
									stops,
									colors,
									this.cycle.toAwt(),
									this.interpolationMethod.toAwt(),
									t1
								)
							} else {
								defaultColor
							}
						}
					}

				}
				is Context2d.BitmapPaint -> {
					object : java.awt.TexturePaint(
						this.bitmap.toAwt(),
						Rectangle2D.Double(0.0, 0.0, this.bitmap.width.toDouble(), this.bitmap.height.toDouble())
					) {
						override fun createContext(
							cm: ColorModel?,
							deviceBounds: Rectangle?,
							userBounds: Rectangle2D?,
							xform: AffineTransform?,
							hints: RenderingHints?
						): PaintContext {
							val out = xform ?: AffineTransform()
							out.concatenate(t1)
							return super.createContext(cm, deviceBounds, userBounds, out, this@AwtContext2dRender.hints)
						}
					}
				}
				else -> java.awt.Color(Colors.BLACK.value)
			}
		}
		else -> java.awt.Color(Colors.BLACK.value)
	}

	fun LineCap.toAwt() = when (this) {
		LineCap.BUTT -> BasicStroke.CAP_BUTT
		LineCap.ROUND -> BasicStroke.CAP_ROUND
		LineCap.SQUARE -> BasicStroke.CAP_SQUARE
	}

	fun LineJoin.toAwt() = when (this) {
		LineJoin.SQUARE -> java.awt.BasicStroke.JOIN_BEVEL
		LineJoin.MITER -> java.awt.BasicStroke.JOIN_MITER
		LineJoin.ROUND -> java.awt.BasicStroke.JOIN_ROUND
	}

    fun Font.toAwt() = when (this) {
        is Font.System -> this.toAwt()
        else -> TODO("Unsupported font toAwt!")
    }
	fun Font.System.toAwt() = java.awt.Font(this.name, java.awt.Font.PLAIN, this.size.toInt())

	inline fun Graphics2D.keepTransform(callback: () -> Unit) {
		val old = AffineTransform(this.transform)
		try {
			callback()
		} finally {
			this.transform = old
		}
	}

	fun applyState(state: Context2d.State, fill: Boolean) {
		val t = state.transform
		awtTransform.setTransform(t.a, t.b, t.c, t.d, t.tx, t.ty)
		g.transform = awtTransform
		g.clip = state.clip?.toJava2dPath()
		if (fill) {
			g.paint = state.fillStyle.toAwt(awtTransform)
		} else {
			//val scale = Math.max(t.a, t.d)
			val strokeSize = (state.lineWidth).toFloat()
			//val aStrokeSize = if (strokeSize / scale < 1.0) {
			//	scale.toFloat()
			//} else {
			//	strokeSize
			//}
			//println("applyState: $strokeSize, lineWidth=${state.lineWidth}, lineScaleMode=${state.lineScaleMode}")
			g.stroke = BasicStroke(
				strokeSize,
				state.lineCap.toAwt(),
				state.lineJoin.toAwt(),
				state.miterLimit.toFloat()
			)
			g.paint = state.strokeStyle.toAwt(awtTransform)
		}
		val comp = AlphaComposite.SRC_OVER
		g.composite = if (state.globalAlpha == 1.0) AlphaComposite.getInstance(comp) else AlphaComposite.getInstance(
			comp,
			state.globalAlpha.toFloat()
		)
	}

	override fun render(state: Context2d.State, fill: Boolean) {
		if (state.path.isEmpty()) return

		applyState(state, fill)

		val awtPaths = state.path.toJava2dPaths()
		for (awtPath in awtPaths) {
			g.setRenderingHints(hints)
			if (fill) {
				g.fill(awtPath)
			} else {
				g.draw(awtPath)
			}
		}
	}

	override fun renderText(
		state: Context2d.State,
		font: Font,
		text: String,
		x: Double,
		y: Double,
		fill: Boolean
	) {
		if (text.isEmpty()) return
		applyState(state, fill)
		val frc = g.fontRenderContext
		val tl = TextLayout(text, font.toAwt(), frc)
		val at = AffineTransform()
		val fm = g.fontMetrics
		val bounds = tl.bounds
		val metrics = TextMetrics()
		getBounds(font, text, metrics)
		//println("text: $text")
		//println("leading:${fm.leading}, ascent:${fm.ascent}, maxAscent:${fm.maxAscent}")
		//println(metrics.bounds)
		val baseline = metrics.bounds.y
		val oy: Double = state.verticalAlign.getOffsetY(bounds.height, baseline.toDouble())
		//val oy = 0.0
		val ox: Double = state.horizontalAlign.getOffsetX(bounds.width)
		//println("$ox, $oy")
		//println("${tl.baseline}")
		//println("${fm.ascent}")
		//println("${fm.ascent + fm.descent}")
		//at.translate(x - ox, y - baseline + oy)
		at.translate(x - ox, y - baseline + oy)
		//println("translate: ${x - ox}, ${y - oy}")
		val outline = tl.getOutline(at)
		g.setRenderingHints(hints)
		if (fill) {
			g.fill(outline)
		} else {
			g.draw(outline)
		}
	}

	override fun getBounds(font: Font, text: String, out: TextMetrics) {
		val fm = g.getFontMetrics(font.toAwt())
		val bounds = fm.getStringBounds(text, g)
		out.bounds.setTo(bounds.x, bounds.y, bounds.width, bounds.height)
	}
}
