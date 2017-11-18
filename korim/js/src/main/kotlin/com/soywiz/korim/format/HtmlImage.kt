package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.util.OS
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.browser.document

object HtmlImage {
	fun createHtmlCanvas(width: Int, height: Int): HTMLCanvasElement {
		val canvas: HTMLCanvasElement = HtmlCanvas.createCanvas(width, height)
		canvas.width = width
		canvas.height = height
		return canvas
	}

	fun renderToHtmlCanvas(bmpData: IntArray, bmpWidth: Int, bmpHeight: Int, canvas: HTMLCanvasElement): HTMLCanvasElement {
		val bmpDataData = bmpData
		val pixelCount = bmpData.size
		val ctx = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
		val idata = ctx.createImageData(bmpWidth.toDouble(), bmpHeight.toDouble())
		val idataData = idata.data
		var m = 0
		for (n in 0 until pixelCount) {
			val c = bmpDataData[n]
			idataData[m++] = ((c ushr 0) and 0xFF).toByte()
			idataData[m++] = ((c ushr 8) and 0xFF).toByte()
			idataData[m++] = ((c ushr 16) and 0xFF).toByte()
			idataData[m++] = ((c ushr 24) and 0xFF).toByte()
		}
		ctx.putImageData(idata, 0.0, 0.0)
		return canvas
	}

	fun renderToHtmlCanvas(bmp: Bitmap32, canvas: HTMLCanvasElement): HTMLCanvasElement {
		return renderToHtmlCanvas(bmp.data, bmp.width, bmp.height, canvas)
	}

	fun renderHtmlCanvasIntoBitmap(canvas: HTMLCanvasElement, out: IntArray): Unit {
		val width = canvas.width
		val height = canvas.height
		val len = width * height
		val ctx = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
		val data = ctx.getImageData(0.0, 0.0, width.toDouble(), height.toDouble())
		val ddata = data.data
		var m = 0
		for (n in 0 until len) {
			val r = ddata[m++].toInt()
			val g = ddata[m++].toInt()
			val b = ddata[m++].toInt()
			val a = ddata[m++].toInt()
			out[n] = (r shl 0) or (g shl 8) or (b shl 16) or (a shl 24)
		}
		//console.log(out);
	}

	fun renderHtmlCanvasIntoBitmap(canvas: HTMLCanvasElement, bmp: Bitmap32): Unit {
		renderHtmlCanvasIntoBitmap(canvas, bmp.data)
	}

	fun bitmapToHtmlCanvas(bmp: Bitmap32): HTMLCanvasElement {
		return renderToHtmlCanvas(bmp, createHtmlCanvas(bmp.width, bmp.height))
	}

	fun htmlCanvasToDataUrl(canvas: HTMLCanvasElement): String = canvas.toDataURL()

	fun htmlCanvasClear(canvas: HTMLCanvasElement): Unit {
		val ctx = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
		ctx.clearRect(
			0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble()
		)
	}

	fun htmlCanvasSetSize(canvas: HTMLCanvasElement, width: Int, height: Int): HTMLCanvasElement {
		canvas.width = width
		canvas.height = height
		return canvas
	}
}

fun Bitmap.toHtmlNative(): CanvasNativeImage = when (this) {
	is CanvasNativeImage -> this
	else -> CanvasNativeImage(HtmlImage.bitmapToHtmlCanvas(this.toBMP32()))
}