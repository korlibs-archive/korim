package com.soywiz.korim.html

import com.jtransc.js.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.NamedColors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.NativeImageFormatProvider
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
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

	override fun getContext2d(): Context2d = Context2d(CanvasContext2d(canvas))
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

class CanvasContext2d(val canvas: JsDynamic?) : Context2d.Renderer {
	val ctx = canvas.methods["getContext"]("2d")

	fun Context2d.Paint.toJsStr(): String {
		return when (this) {
			is Context2d.None -> "none"
			is Context2d.Color -> NamedColors.toHtmlString(this.color)
			else -> TODO()
		}
	}

	override fun render(state: Context2d.State, fill: Boolean) {
		state.path.visit(object : GraphicsPath.Visitor {
			override fun close() {
				ctx.methods["closePath"]()
			}

			override fun moveTo(x: Double, y: Double) {
				ctx.methods["moveTo"](x, y)
			}

			override fun lineTo(x: Double, y: Double) {
				ctx.methods["lineTo"](x, y)
			}

			override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) {
				ctx.methods["quadraticCurveTo"](cx, cy, ax, ay)
			}

			override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) {
				ctx.methods["curveTo"](cx1, cy1, cx2, cy2, ax, ay)
			}
		})

		if (fill) {
			ctx["fillStyle"] = state.fillStyle.toJsStr()
			ctx.methods["fill"]()
		} else {
			ctx["lineWidth"] = state.lineWidth
			ctx["strokeStyle"] = state.strokeStyle.toJsStr()
			ctx.methods["stroke"]()
		}
	}
}