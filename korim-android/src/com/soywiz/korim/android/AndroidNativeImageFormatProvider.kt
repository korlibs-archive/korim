package com.soywiz.korim.android

import android.graphics.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.BGRA
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.NativeImageFormatProvider
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath

fun Bitmap.toAndroidBitmap(): android.graphics.Bitmap {
	if (this is AndroidNativeImage) return this.androidBitmap
	val bmp32 = this.toBMP32()
	return android.graphics.Bitmap.createBitmap(bmp32.data, 0, bmp32.width, bmp32.width, bmp32.height, android.graphics.Bitmap.Config.ARGB_8888)
}

class AndroidNativeImage(val androidBitmap: android.graphics.Bitmap) : NativeImage(androidBitmap.width, androidBitmap.height, androidBitmap) {
	override fun toNonNativeBmp(): Bitmap {
		val out = IntArray(width * height)
		androidBitmap.getPixels(out, 0, width, 0, 0, width, height)
		return Bitmap32(width, height, out)
	}

	override fun getContext2d(): Context2d = Context2d(AndroidContext2dRenderer(androidBitmap))
}

class AndroidContext2dRenderer(val bmp: android.graphics.Bitmap) : Context2d.Renderer {
	val paint = Paint()
	val canvas = Canvas(bmp)
	val matrixValues = FloatArray(9)
	var androidMatrix = android.graphics.Matrix()

	fun GraphicsPath.toAndroid(): Path {
		val out = Path()

		out.fillType = when (this.winding) {
			GraphicsPath.Winding.EVEN_ODD -> Path.FillType.EVEN_ODD
			GraphicsPath.Winding.NON_ZERO -> Path.FillType.INVERSE_EVEN_ODD
		}
		//kotlin.io.println("Path:")
		this.visit(object : GraphicsPath.Visitor {
			override fun moveTo(x: Double, y: Double) {
				//kotlin.io.println("moveTo($x,$y)")
				out.moveTo(x.toFloat(), y.toFloat())
			}

			override fun lineTo(x: Double, y: Double) {
				//kotlin.io.println("lineTo($x,$y)")
				out.lineTo(x.toFloat(), y.toFloat())
			}

			override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) {
				//kotlin.io.println("quadTo($cx,$cy,$ax,$ay)")
				out.quadTo(cx.toFloat(), cy.toFloat(), ax.toFloat(), ay.toFloat())
			}

			override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) {
				//kotlin.io.println("cubicTo($cx1,$cy1,$cx2,$cy2,$ax,$ay)")
				out.cubicTo(cx1.toFloat(), cy1.toFloat(), cx2.toFloat(), cy2.toFloat(), ax.toFloat(), ay.toFloat())
			}

			override fun close() {
				//kotlin.io.println("close()")
				out.close()
			}
		})
		//kotlin.io.println("/Path")
		return out
	}

	override fun render(state: Context2d.State, fill: Boolean) {
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

		canvas.save()
		try {
			canvas.matrix = androidMatrix
			if (state.clip != null) {
				canvas.clipPath(state.clip?.toAndroid())
			}

			if (fill) {
				paint.style = android.graphics.Paint.Style.FILL
				val fillStyle = state.fillStyle
				when (fillStyle) {
					is Context2d.None -> Unit
					is Context2d.Color -> paint.color = BGRA.packRGBA(fillStyle.color)
				}
			} else {
				paint.style = android.graphics.Paint.Style.STROKE
				val strokeStyle = state.strokeStyle
				when (strokeStyle) {
					is Context2d.None -> Unit
					is Context2d.Color -> paint.color = BGRA.packRGBA(strokeStyle.color)
				}
			}

			paint.strokeWidth = state.lineWidth.toFloat()
			//println("-----------------")
			//println(canvas.matrix)
			//println(state.path.toAndroid())
			//println(paint.style)
			//println(paint.color)
			canvas.drawPath(state.path.toAndroid(), paint)
		} finally {
			canvas.restore()
		}
	}
}

class AndroidNativeImageFormatProvider : NativeImageFormatProvider() {
	override fun create(width: Int, height: Int): NativeImage {
		val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
		//bmp.setPixels()
		return AndroidNativeImage(bmp)
	}
	suspend override fun decode(data: ByteArray): NativeImage = AndroidNativeImage(BitmapFactory.decodeByteArray(data, 0, data.size))
}