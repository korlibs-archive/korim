package com.soywiz.korim.html

import com.jtransc.annotation.JTranscMethodBody
import com.jtransc.js.*
import com.soywiz.korim.bitmap.Bitmap32

object HtmlImage {
	fun createHtmlCanvas(width: Int, height: Int): JsDynamic {
		val canvas = document.getMethod("createElement")("canvas")
		canvas["width"] = width
		canvas["height"] = height
		return canvas!!
	}

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

	fun bitmapToHtmlCanvas(bmp: Bitmap32): JsDynamic {
		return renderToHtmlCanvas(bmp, createHtmlCanvas(bmp.width, bmp.height));
	}

	fun htmlCanvasToDataUrl(canvas: JsDynamic): String {
		return canvas.getMethod("toDataURL")().toJavaString()
	}

	fun htmlCanvasClear(canvas: JsDynamic): Unit {
		val ctx = canvas.getMethod("getContext")("2d")
		ctx.getMethod("clearRect")(0, 0, canvas["width"], canvas["height"])
	}

	fun htmlCanvasSetSize(canvas: JsDynamic, width: Int, height: Int): JsDynamic {
		canvas["width"] = width
		canvas["height"] = height
		return canvas
	}
}