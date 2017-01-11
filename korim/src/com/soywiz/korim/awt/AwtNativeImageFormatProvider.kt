package com.soywiz.korim.awt

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.BGRA
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.NativeImageFormatProvider
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

class AwtNativeImageFormatProvider : NativeImageFormatProvider() {
	suspend override fun decode(data: ByteArray): NativeImage {
		return AwtNativeImage(awtReadImage(data))
	}

	override fun create(width: Int, height: Int): NativeImage {
		return AwtNativeImage(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB))
	}
}

class AwtNativeImage(val awtImage: BufferedImage) : NativeImage(awtImage.width, awtImage.height, awtImage) {
	override fun toNonNativeBmp(): Bitmap = awtImage.toBMP32()
	override fun getContext2d(): Context2d = Context2d(AwtContext2d(awtImage))
}

class AwtContext2d(val awtImage: BufferedImage) : Context2d.Renderer {
	val awtTransform = AffineTransform()
	val g = awtImage.createGraphics().apply {
		this.addRenderingHints(mapOf(KEY_ANTIALIASING to java.awt.RenderingHints.VALUE_ANTIALIAS_ON))
	}
	val paint = Color(BGRA.packRGBA(Colors.RED), true)


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

	fun Context2d.Paint.toAwt(): java.awt.Paint = when (this) {
		is Context2d.Color -> java.awt.Color(BGRA.packRGBA(this.color), true)
		else -> TODO()
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

	override fun render(state: Context2d.State, fill: Boolean) {
		if (state.path.isEmpty()) return
		val t = state.transform
		awtTransform.setTransform(t.a, t.b, t.c, t.d, t.tx, t.ty)
		g.transform = awtTransform
		g.clip = state.clip?.toJava2dPath()
		val awtPath = state.path.toJava2dPath()
		if (fill) {
			g.paint = state.fillStyle.toAwt()
			g.fill(awtPath)
		} else {
			g.stroke = BasicStroke(
				state.lineWidth.toFloat(),
				state.lineCap.toAwt(),
				state.lineJoin.toAwt(),
				state.miterLimit.toFloat()
			)
			g.paint = state.strokeStyle.toAwt()
			g.draw(awtPath)
		}
	}
}