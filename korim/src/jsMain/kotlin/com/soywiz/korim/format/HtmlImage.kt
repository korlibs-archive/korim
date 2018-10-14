package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.*

object HtmlImage {
	fun createHtmlCanvas(width: Int, height: Int): HTMLCanvasElement {
		val canvas: HTMLCanvasElement = HtmlCanvas.createCanvas(width, height)
		canvas.width = width
		canvas.height = height
		return canvas
	}

	fun renderToHtmlCanvas(
		bmpData: RgbaArray,
		bmpWidth: Int,
		bmpHeight: Int,
		canvas: HTMLCanvasElement
	): HTMLCanvasElement {
		val pixelCount = bmpData.size
		val ctx = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
		val idata = ctx.createImageData(bmpWidth.toDouble(), bmpHeight.toDouble())
		val idataData = idata.data
		var m = 0
		for (n in 0 until pixelCount) {
			val c = bmpData.array[n]

			// @TODO: Kotlin.JS bug Clamped Array should be int inst@TODO: Kotlin.JS bug Clamped Array should be int instead of Byte
			idataData[m++] = RGBA.getR(c).asDynamic()
			idataData[m++] = RGBA.getG(c).asDynamic()
			idataData[m++] = RGBA.getB(c).asDynamic()
			idataData[m++] = RGBA.getA(c).asDynamic()
		}
		ctx.putImageData(idata, 0.0, 0.0)
		return canvas
	}

	fun renderToHtmlCanvas(bmp: Bitmap32, canvas: HTMLCanvasElement): HTMLCanvasElement {
		val data = if (bmp.premult) {
			RGBA.depremultiplyFast(RgbaArray(bmp.data.array.copyOf()))
		} else {
			bmp.data
		}
		return renderToHtmlCanvas(data, bmp.width, bmp.height, canvas)
	}

	fun renderHtmlCanvasIntoBitmap(canvas: HTMLCanvasElement, out: RgbaArray): Unit {
		val width = canvas.width
		val height = canvas.height
		val len = width * height
		val ctx = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
		val data = ctx.getImageData(0.0, 0.0, width.toDouble(), height.toDouble())
		val ddata = data.data
		var m = 0
		for (n in 0 until len) {
			val r = ddata[m++].toInt() and 0xFF
			val g = ddata[m++].toInt() and 0xFF
			val b = ddata[m++].toInt() and 0xFF
			val a = ddata[m++].toInt() and 0xFF
			out.array[n] = RGBA.packFast(r, g, b, a)
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

fun Bitmap.toHtmlNative(): HtmlNativeImage = when (this) {
	is HtmlNativeImage -> this
	else -> HtmlNativeImage(HtmlImage.bitmapToHtmlCanvas(this.toBMP32()))
}