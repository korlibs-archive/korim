package com.soywiz.korim.html

import com.jtransc.js.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.format.NativeImageFormatProvider
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.vfs.js.jsObject
import kotlin.coroutines.CoroutineIntrinsics
import kotlin.coroutines.suspendCoroutine

class CanvasNativeImage(val canvas: JsDynamic?) : NativeImage(canvas["width"].toInt(), canvas["height"].toInt(), canvas) {
	override fun toNonNativeBmp(): Bitmap {
		val data = IntArray(width * height)
		BrowserNativeImageFormatProvider.BrowserImage.imgData(canvas, data)
		return Bitmap32(width, height, data)
	}
}

class BrowserNativeImageFormatProvider : NativeImageFormatProvider() {
	override fun create(width: Int, height: Int): NativeImage {
		val canvas = document.methods["createElement"]("canvas")
		canvas["width"] = width
		canvas["height"] = height
		return CanvasNativeImage(canvas)
	}

	suspend override fun decode(data: ByteArray): NativeImage = asyncFun {
		CanvasNativeImage(BrowserImage.decodeToCanvas(data))
	}

	@Suppress("unused")
	object BrowserImage {
		suspend fun decodeToCanvas(bytes: ByteArray): JsDynamic? = suspendCoroutine { c ->
			val blob = jsNew("Blob", jsArray(bytes), jsObject("type" to "image/png"))
			val blobURL = global["URL"].methods["createObjectURL"](blob);

			val img = jsNew("Image")
			img["onload"] = jsFunctionRaw0 {
				val canvas = document.methods["createElement"]("canvas");
				canvas["width"] = img["width"]
				canvas["height"] = img["height"]
				val ctx = canvas.methods["getContext"]("2d");
				ctx.methods["drawImage"](img, 0, 0);
				global["URL"].methods["revokeObjectURL"](blobURL);
				c.resume(canvas)
			};
			img["onerror"] = jsFunctionRaw0 {
				c.resumeWithException(RuntimeException("error loading image"))
			};
			img["src"] = blobURL;
		}

		fun imgData(canvas: JsDynamic?, out: IntArray): Unit {
			HtmlImage.renderHtmlCanvasIntoBitmap(canvas, out)
		}

		@Suppress("unused")
		private fun getSuspended() = CoroutineIntrinsics.SUSPENDED
	}
}