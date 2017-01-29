package com.soywiz.korim.awt

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.NativeImageFormatProvider
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Font
import java.awt.RenderingHints
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

class AwtNativeImageFormatProvider : NativeImageFormatProvider() {
	suspend override fun decode(data: ByteArray): NativeImage {
		return AwtNativeImage(awtReadImage(data))
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
		val winding = if (winding == GraphicsPath.Winding.EVEN_ODD) java.awt.geom.GeneralPath.WIND_EVEN_ODD else java.awt.geom.GeneralPath.WIND_NON_ZERO
		val polyline = java.awt.geom.Path2D.Double(winding)
		this.visit(object : GraphicsPath.Visitor {
			override fun moveTo(x: Double, y: Double) = polyline.moveTo(x, y)
			override fun lineTo(x: Double, y: Double) = polyline.lineTo(x, y)
			override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) = polyline.quadTo(cx, cy, ax, ay)
			override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) = polyline.curveTo(cx1, cy1, cx2, cy2, ax, ay)
			override fun close() = polyline.closePath()
		})
		return polyline
	}

	fun convertColor(c: Int): java.awt.Color = java.awt.Color(RGBA.getR(c), RGBA.getG(c), RGBA.getB(c), RGBA.getA(c))

	fun Context2d.Paint.toAwt(): java.awt.Paint = when (this) {
		is Context2d.Color -> convertColor(this.color)
		is Context2d.LinearGradient -> java.awt.LinearGradientPaint(
			this.x0.toFloat(),
			this.y0.toFloat(),
			this.x1.toFloat(),
			this.y1.toFloat(),
			this.stops.map(Double::toFloat).toFloatArray(),
			this.colors.map { convertColor(it) }.toTypedArray()
		)
	//is Context2d.RadialGradient -> java.awt.RadialGradientPaint(
	//	this.x0.toFloat(),
	//	this.y0.toFloat(),
	//	this.x1.toFloat(),
	//	this.y1.toFloat(),
	//	this.stops.map(Double::toFloat).toFloatArray(),
	//	this.colors.map { java.awt.Color(it, true) }.toTypedArray()
	//)
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