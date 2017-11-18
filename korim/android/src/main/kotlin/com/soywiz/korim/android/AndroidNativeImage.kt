package com.soywiz.korim.android

import android.graphics.*
import android.text.TextPaint
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.BGRA
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.VectorPath


fun Bitmap.toAndroidBitmap(): android.graphics.Bitmap {
	if (this is AndroidNativeImage) return this.androidBitmap
	val bmp32 = this.toBMP32()
	return android.graphics.Bitmap.createBitmap(bmp32.data, 0, bmp32.width, bmp32.width, bmp32.height, android.graphics.Bitmap.Config.ARGB_8888)
}

class AndroidNativeImage(val androidBitmap: android.graphics.Bitmap) : NativeImage(androidBitmap.width, androidBitmap.height, androidBitmap, premultiplied = false) {
	override val name: String = "AndroidNativeImage"

	override fun toNonNativeBmp(): Bitmap {
		val out = IntArray(width * height)
		androidBitmap.getPixels(out, 0, width, 0, 0, width, height)
		return Bitmap32(width, height, out, premult = premult)
	}

	override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(AndroidContext2dRenderer(androidBitmap))
}

class AndroidContext2dRenderer(val bmp: android.graphics.Bitmap) : Context2d.Renderer() {
	override val width: Int get() = bmp.width
	override val height: Int get() = bmp.height
	//val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
	val paint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG or TextPaint.ANTI_ALIAS_FLAG or TextPaint.SUBPIXEL_TEXT_FLAG).apply {
		hinting = Paint.HINTING_ON
		isAntiAlias = true
		isFilterBitmap = true
		isDither = true
	}
	val canvas = Canvas(bmp)
	val matrixValues = FloatArray(9)
	var androidMatrix = android.graphics.Matrix()

	fun GraphicsPath.toAndroid(): Path {
		val out = Path()

		out.fillType = when (this.winding) {
			VectorPath.Winding.EVEN_ODD -> Path.FillType.EVEN_ODD
			VectorPath.Winding.NON_ZERO -> Path.FillType.INVERSE_EVEN_ODD
			else -> Path.FillType.EVEN_ODD
		}
		//kotlin.io.println("Path:")
		this.visitCmds(
			moveTo = { x, y -> out.moveTo(x.toFloat(), y.toFloat()) },
			lineTo = { x, y -> out.lineTo(x.toFloat(), y.toFloat()) },
			quadTo = { cx, cy, ax, ay -> out.quadTo(cx.toFloat(), cy.toFloat(), ax.toFloat(), ay.toFloat()) },
			cubicTo = { cx1, cy1, cx2, cy2, ax, ay -> out.cubicTo(cx1.toFloat(), cy1.toFloat(), cx2.toFloat(), cy2.toFloat(), ax.toFloat(), ay.toFloat()) },
			close = { out.close() }
		)
		//kotlin.io.println("/Path")
		return out
	}

	fun convertPaint(c: Context2d.Paint, out: Paint) {
		when (c) {
			is Context2d.None -> {
				out.shader = null
			}
			is Context2d.Color -> {
				out.color = BGRA.packRGBA(c.color)
				out.shader = null
			}
			is Context2d.Gradient -> {
				when (c.kind) {
					Context2d.Gradient.Kind.LINEAR ->
						out.shader = LinearGradient(
							c.x0.toFloat(), c.y0.toFloat(),
							c.x1.toFloat(), c.y1.toFloat(),
							c.colors.toIntArray(), c.stops.map(Double::toFloat).toFloatArray(), Shader.TileMode.CLAMP
						)
					Context2d.Gradient.Kind.RADIAL ->
						out.shader = RadialGradient(
							c.x1.toFloat(), c.y1.toFloat(), c.r1.toFloat(),
							c.colors.toIntArray(), c.stops.map(Double::toFloat).toFloatArray(), Shader.TileMode.CLAMP
						)
				}

			}
		}
	}

	inline fun <T> keep(callback: () -> T): T {
		canvas.save()
		try {
			return callback()
		} finally {
			canvas.restore()
		}
	}

	private fun setState(state: Context2d.State, fill: Boolean) {
		val transform = state.transform
		matrixValues[Matrix.MSCALE_X] = transform.a.toFloat()
		matrixValues[Matrix.MSKEW_X] = transform.b.toFloat()
		matrixValues[Matrix.MSKEW_Y] = transform.c.toFloat()
		matrixValues[Matrix.MSCALE_Y] = transform.d.toFloat()
		matrixValues[Matrix.MTRANS_X] = transform.tx.toFloat()
		matrixValues[Matrix.MTRANS_Y] = transform.ty.toFloat()
		matrixValues[Matrix.MPERSP_0] = 0f
		matrixValues[Matrix.MPERSP_1] = 0f
		matrixValues[Matrix.MPERSP_2] = 1f
		androidMatrix.setValues(matrixValues)
		canvas.matrix = androidMatrix
		paint.strokeWidth = state.lineWidth.toFloat()
	}

	override fun render(state: Context2d.State, fill: Boolean) {
		setState(state, fill)

		keep {
			if (state.clip != null) canvas.clipPath(state.clip?.toAndroid())

			if (fill) {
				paint.style = android.graphics.Paint.Style.FILL
				convertPaint(state.fillStyle, paint)
			} else {
				paint.style = android.graphics.Paint.Style.STROKE
				convertPaint(state.strokeStyle, paint)
			}

			//println("-----------------")
			//println(canvas.matrix)
			//println(state.path.toAndroid())
			//println(paint.style)
			//println(paint.color)
			canvas.drawPath(state.path.toAndroid(), paint)
		}
	}

	override fun renderText(state: Context2d.State, font: Context2d.Font, text: String, x: Double, y: Double, fill: Boolean) {
		val metrics = Context2d.TextMetrics()
		val bounds = metrics.bounds
		paint.typeface = Typeface.create(font.name, Typeface.NORMAL)
		paint.textSize = font.size.toFloat()
		val fm = paint.fontMetrics
		getBounds(font, text, metrics)

		val baseline = fm.ascent + fm.descent

		val ox = state.horizontalAlign.getOffsetX(bounds.width)
		val oy = state.verticalAlign.getOffsetY(bounds.height, baseline.toDouble())

		//val tp = TextPaint(TextPaint.ANTI_ALIAS_FLAG)

		canvas.drawText(text, 0, text.length, (x - ox).toFloat(), (y + baseline - oy).toFloat(), paint)
	}

	override fun getBounds(font: Context2d.Font, text: String, out: Context2d.TextMetrics) {
		val rect = Rect()
		paint.getTextBounds(text, 0, text.length, rect)
		out.bounds.setTo(rect.left.toDouble(), rect.top.toDouble(), rect.width().toDouble(), rect.height().toDouble())
	}

	override fun drawImage(image: Bitmap, x: Int, y: Int, width: Int, height: Int, transform: Matrix2d) {
		TODO()
	}
}

