package com.soywiz.korim.awt

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.BGRA
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.NativeImageFormatProvider
import com.soywiz.korim.geom.Matrix2d
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Polygon
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
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
	override fun getContext2d(): Context2d = AwtContext2d(awtImage)
}

class AwtContext2d(val awtImage: BufferedImage) : Context2d() {
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
			override fun bezierTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) = polyline.curveTo(cx1, cy1, cx2, cy2, ax, ay)
			override fun close() = polyline.closePath()
		})
		return polyline
	}

	fun Paint.toAwt(): java.awt.Paint = when (this) {
		is Color -> java.awt.Color(BGRA.packRGBA(this.color), true)
		else -> TODO()
	}

	fun LineCap.toAwt() = when (this) {
		LineCap.BUTT -> BasicStroke.CAP_BUTT
		LineCap.ROUND -> BasicStroke.CAP_ROUND
		LineCap.SQUARE -> BasicStroke.CAP_SQUARE
	}

	fun LineJoin.toAwt() = when (this) {
		LineJoin.BEVEL -> java.awt.BasicStroke.JOIN_BEVEL
		LineJoin.MITER -> java.awt.BasicStroke.JOIN_MITER
		LineJoin.ROUND -> java.awt.BasicStroke.JOIN_ROUND
	}

	override fun render(state: State, fill: Boolean) {
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