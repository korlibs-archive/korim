package com.soywiz.korim.html

import com.jtransc.annotation.JTranscMethodBody
import com.jtransc.js.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage

object HtmlImage {
	fun createHtmlCanvas(width: Int, height: Int): JsDynamic {
		val canvas = document.call("createElement", "canvas")
		canvas["width"] = width
		canvas["height"] = height
		return canvas!!
	}

	// @NOTE: This is performance critical and this is why it is described as plain JS
	@JTranscMethodBody(target = "js", value = """
		var bmpData = p0, bmpWidth = p1, bmpHeight = p2, canvas = p3;
		var bmpDataData = bmpData.data;
		var pixelCount = bmpData.length;
		var ctx = canvas.getContext('2d');
		var idata = ctx.createImageData(bmpWidth, bmpHeight);
		var idataData = idata.data;
		var m = 0;
		for (var n = 0; n < pixelCount; n++) {
			var c = bmpDataData[n];
			idataData[m++] = (c >>  0) & 0xFF;
			idataData[m++] = (c >>  8) & 0xFF;
			idataData[m++] = (c >> 16) & 0xFF;
			idataData[m++] = (c >> 24) & 0xFF;
		}
		ctx.putImageData(idata, 0, 0);
		return canvas;
	""")
	external fun renderToHtmlCanvas(bmpData: IntArray, bmpWidth: Int, bmpHeight: Int, canvas: JsDynamic): JsDynamic

	fun renderToHtmlCanvas(bmp: Bitmap32, canvas: JsDynamic): JsDynamic {
		return renderToHtmlCanvas(bmp.data, bmp.width, bmp.height, canvas)
	}

	@JTranscMethodBody(target = "js", value = """
		var canvas = p0, out = p1;
		var width = canvas.width, height = canvas.height, len = width * height;
		var ctx = canvas.getContext('2d')
		var data = ctx.getImageData(0, 0, width, height);
		var ddata = data.data;
		var m = 0;
		for (var n = 0; n < len; n++) {
			var r = ddata[m++];
			var g = ddata[m++];
			var b = ddata[m++];
			var a = ddata[m++];
			out.data[n] = (r << 0) | (g << 8) | (b << 16) | (a << 24);
		}
		//console.log(out);
	""")
	external fun renderHtmlCanvasIntoBitmap(canvas: JsDynamic?, bmp: IntArray): Unit

	fun renderHtmlCanvasIntoBitmap(canvas: JsDynamic?, bmp: Bitmap32): Unit {
		renderHtmlCanvasIntoBitmap(canvas, bmp.data)
	}

	fun bitmapToHtmlCanvas(bmp: Bitmap32): JsDynamic {
		return renderToHtmlCanvas(bmp, createHtmlCanvas(bmp.width, bmp.height));
	}

	fun htmlCanvasToDataUrl(canvas: JsDynamic): String = canvas.call("toDataURL").toJavaString()

	fun htmlCanvasClear(canvas: JsDynamic): Unit {
		canvas.call("getContext", "2d").call("clearRect", 0, 0, canvas["width"], canvas["height"])
	}

	fun htmlCanvasSetSize(canvas: JsDynamic, width: Int, height: Int): JsDynamic {
		canvas["width"] = width
		canvas["height"] = height
		return canvas
	}
}

fun Bitmap.toHtmlNative(): CanvasNativeImage = when(this) {
	is CanvasNativeImage -> this
	else -> CanvasNativeImage(HtmlImage.bitmapToHtmlCanvas(this.toBMP32()))
}