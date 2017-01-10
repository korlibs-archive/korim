package com.soywiz.korim.android

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.format.NativeImageFormatProvider
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath

fun Bitmap.toAndroidBitmap(): android.graphics.Bitmap {
	val bmp32 = this.toBMP32()
	return android.graphics.Bitmap.createBitmap(bmp32.data, 0, bmp32.width, bmp32.width, bmp32.height, android.graphics.Bitmap.Config.ARGB_8888)
}

class AndroidNativeImage(val androidBitmap: android.graphics.Bitmap) : NativeImage(androidBitmap.width, androidBitmap.height, androidBitmap) {
	override fun toNonNativeBmp(): Bitmap {
		val out = IntArray(width * height)
		androidBitmap.getPixels(out, 0, width, 0, 0, width, height)
		return Bitmap32(width, height, out)
	}

	override fun getContext2d(): Context2d {
		return AndroidContext2d(androidBitmap)
	}
}

class AndroidContext2d(val bmp: android.graphics.Bitmap) : Context2d() {
	val paint = Paint()
	val canvas = Canvas(bmp)
	val matrixValues = FloatArray(6)
	var androidMatrix = android.graphics.Matrix()

	var dirtyTransform = true

	override fun updatedTransform() {
		dirtyTransform = true
	}

	private fun setMatrixIfRequired() {
		if (!dirtyTransform) return
		matrixValues[0] = transform.a.toFloat()
		matrixValues[1] = transform.b.toFloat()
		matrixValues[2] = transform.c.toFloat()
		matrixValues[3] = transform.d.toFloat()
		matrixValues[4] = transform.tx.toFloat()
		matrixValues[5] = transform.ty.toFloat()
		androidMatrix.setValues(matrixValues)
		canvas.matrix = androidMatrix
		dirtyTransform = false
	}

	fun convertPath(path: GraphicsPath): Path {
		val out = Path()
		path.visit(object : GraphicsPath.Visitor {
			override fun moveTo(x: Double, y: Double) {
				out.moveTo(x.toFloat(), y.toFloat())
			}

			override fun lineTo(x: Double, y: Double) {
				out.lineTo(x.toFloat(), y.toFloat())
			}

			override fun curveTo(cx: Double, cy: Double, ax: Double, ay: Double) {
				out.quadTo(cx.toFloat(), cy.toFloat(), ax.toFloat(), ay.toFloat())
			}

		})
		return out
	}

	override fun drawPath(path: GraphicsPath) {
		canvas.drawPath(convertPath(path), paint)
	}
}

class AndroidNativeImageFormatProvider : NativeImageFormatProvider() {
	override fun create(width: Int, height: Int): NativeImage = AndroidNativeImage(android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888))
	suspend override fun decode(data: ByteArray): NativeImage = AndroidNativeImage(BitmapFactory.decodeByteArray(data, 0, data.size))
}