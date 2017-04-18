package com.soywiz.korim.awt

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.NativeImageFormatProvider
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
import com.soywiz.korma.geom.VectorPath
import java.awt.*
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.image.ColorModel

class AwtNativeImageFormatProvider : NativeImageFormatProvider() {
	suspend override fun decode(data: ByteArray): NativeImage {
		return AwtNativeImage(awtReadImageInWorker(data))
	}

	override fun create(width: Int, height: Int): NativeImage {
		return AwtNativeImage(BufferedImage(Math.max(width, 1), Math.max(height, 1), BufferedImage.TYPE_INT_ARGB))
	}

	override suspend fun display(bitmap: Bitmap): Unit {
		awtShowImageAndWait(bitmap)
	}
}

class AwtNativeImage(val awtImage: BufferedImage) : NativeImage(awtImage.width, awtImage.height, awtImage) {
	override fun toNonNativeBmp(): Bitmap = awtImage.toBMP32()
	override fun getContext2d(): Context2d = Context2d(AwtContext2d(awtImage))
}

class AwtContext2d(val awtImage: BufferedImage) : Context2d.Renderer() {
	val awtTransform = AffineTransform()
	val g = awtImage.createGraphics().apply {
		addRenderingHints(mapOf(
			KEY_ANTIALIASING to java.awt.RenderingHints.VALUE_ANTIALIAS_ON,
			RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
			RenderingHints.KEY_FRACTIONALMETRICS to RenderingHints.VALUE_FRACTIONALMETRICS_ON
		))
	}

	fun GraphicsPath.toJava2dPath(): java.awt.geom.Path2D.Double? {
		if (this.isEmpty()) return null
		val winding = if (winding == VectorPath.Winding.EVEN_ODD) java.awt.geom.GeneralPath.WIND_EVEN_ODD else java.awt.geom.GeneralPath.WIND_NON_ZERO
		val polyline = java.awt.geom.Path2D.Double(winding)
		this.visitCmds(
			moveTo = { x, y -> polyline.moveTo(x, y) },
			lineTo = { x, y -> polyline.lineTo(x, y) },
			quadTo = { cx, cy, ax, ay -> polyline.quadTo(cx, cy, ax, ay) },
			cubicTo = { cx1, cy1, cx2, cy2, ax, ay -> polyline.curveTo(cx1, cy1, cx2, cy2, ax, ay) },
			close = { polyline.closePath() }
		)
		return polyline
	}

	fun convertColor(c: Int): java.awt.Color = java.awt.Color(RGBA.getR(c), RGBA.getG(c), RGBA.getB(c), RGBA.getA(c))

	fun Context2d.CycleMethod.toAwt() = when (this) {
		Context2d.CycleMethod.NO_CYCLE -> MultipleGradientPaint.CycleMethod.NO_CYCLE
		Context2d.CycleMethod.REPEAT -> MultipleGradientPaint.CycleMethod.REPEAT
		Context2d.CycleMethod.REFLECT -> MultipleGradientPaint.CycleMethod.REFLECT
	}

	fun Context2d.Paint.toAwt(): java.awt.Paint = try {
		this.toAwtUnsafe()
	} catch (e: Throwable) {
		println("Context2d.Paint.toAwt: $e")
		Color.RED
	}

	fun Context2d.Paint.toAwtUnsafe(): java.awt.Paint = when (this) {
		is Context2d.Color -> convertColor(this.color)
		is Context2d.Gradient -> {
			val pairs = this.stops.map(Double::toFloat).zip(this.colors.map { convertColor(it) }).distinctBy { it.first }
			val stops = pairs.map { it.first }.toFloatArray()
			val colors = pairs.map { it.second }.toTypedArray()
			val valid = (pairs.size >= 2) && ((x0 != x1) || (y0 != y1))
			val defaultColor = colors.firstOrNull() ?: Color.RED

			when (this) {
				is Context2d.LinearGradient -> {
					if (valid) {
						java.awt.LinearGradientPaint(
							this.x0.toFloat(), this.y0.toFloat(),
							this.x1.toFloat(), this.y1.toFloat(),
							stops,
							colors,
							this.cycle.toAwt()
						)
					} else {
						defaultColor
					}
				}
				is Context2d.RadialGradient -> {
					if (valid) {
						java.awt.RadialGradientPaint(
							this.x0.toFloat(), this.y0.toFloat(), this.r1.toFloat(),
							this.x1.toFloat(), this.y1.toFloat(),
							stops,
							colors,
							this.cycle.toAwt()
						)
					} else {
						defaultColor
					}
				}
				else -> TODO()
			}

		}
		is Context2d.BitmapPaint -> {
			val bmpp = this
			val matrix = bmpp.matrix
			object : java.awt.TexturePaint(
				this.bitmap.toAwt(),
				Rectangle2D.Double(0.0, 0.0, this.bitmap.width.toDouble(), this.bitmap.height.toDouble())
			) {
				override fun createContext(cm: ColorModel?, deviceBounds: Rectangle?, userBounds: Rectangle2D?, xform: AffineTransform?, hints: RenderingHints?): PaintContext {
					val at = AffineTransform()
					at.setTransform(matrix.a, matrix.b, matrix.c, matrix.d, matrix.tx, matrix.ty)
					val out = xform ?: AffineTransform()
					out.concatenate(at)
					return super.createContext(cm, deviceBounds, userBounds, out, hints)
				}
			}
		}
		else -> java.awt.Color(Colors.BLACK)
	}

	fun Context2d.LineCap.toAwt() = when (this) {
		Context2d.LineCap.BUTT -> BasicStroke.CAP_BUTT
		Context2d.LineCap.ROUND -> BasicStroke.CAP_ROUND
		Context2d.LineCap.SQUARE -> BasicStroke.CAP_SQUARE
	}

	fun Context2d.LineJoin.toAwt() = when (this) {
		Context2d.LineJoin.BEVEL -> java.awt.BasicStroke.JOIN_BEVEL
		Context2d.LineJoin.MITER -> java.awt.BasicStroke.JOIN_MITER
		Context2d.LineJoin.ROUND -> java.awt.BasicStroke.JOIN_ROUND
	}

	fun Context2d.Font.toAwt() = Font(this.name, Font.PLAIN, this.size.toInt())

	fun applyState(state: Context2d.State, fill: Boolean) {
		val t = state.transform
		awtTransform.setTransform(t.a, t.b, t.c, t.d, t.tx, t.ty)
		g.transform = awtTransform
		g.clip = state.clip?.toJava2dPath()
		if (fill) {
			g.paint = state.fillStyle.toAwt()
		} else {
			g.stroke = BasicStroke(
				state.lineWidth.toFloat(),
				state.lineCap.toAwt(),
				state.lineJoin.toAwt(),
				state.miterLimit.toFloat()
			)
			g.paint = state.strokeStyle.toAwt()
		}
		val comp = AlphaComposite.SRC_OVER
		g.composite = if (state.globalAlpha == 1.0) AlphaComposite.getInstance(comp) else AlphaComposite.getInstance(comp, state.globalAlpha.toFloat())
	}

	override fun render(state: Context2d.State, fill: Boolean) {
		if (state.path.isEmpty()) return
		applyState(state, fill)
		val awtPath = state.path.toJava2dPath()
		if (fill) {
			g.fill(awtPath)
		} else {
			g.draw(awtPath)
		}
	}

	override fun renderText(state: Context2d.State, font: Context2d.Font, text: String, x: Double, y: Double, fill: Boolean) {
		if (text.isEmpty()) return
		applyState(state, fill)
		val frc = g.fontRenderContext
		val tl = TextLayout(text, font.toAwt(), frc)
		val at = AffineTransform()
		val fm = g.fontMetrics
		val bounds = tl.bounds
		val metrics = Context2d.TextMetrics()
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
		if (fill) {
			g.fill(outline)
		} else {
			g.draw(outline)
		}
	}

	override fun getBounds(font: Context2d.Font, text: String, out: Context2d.TextMetrics) {
		val fm = g.getFontMetrics(font.toAwt())
		val bounds = fm.getStringBounds(text, g)
		out.bounds.setTo(bounds.x, bounds.y, bounds.width, bounds.height)
	}
}