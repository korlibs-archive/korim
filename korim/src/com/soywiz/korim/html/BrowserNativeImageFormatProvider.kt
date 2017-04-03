package com.soywiz.korim.html

import com.jtransc.js.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.NamedColors
import com.soywiz.korim.format.NativeImageFormatProvider
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.coroutine.korioSuspendCoroutine

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
		val canvas = document.call("createElement", "canvas")
		canvas["width"] = width
		canvas["height"] = height
		return CanvasNativeImage(canvas)
	}

	suspend override fun decode(data: ByteArray): NativeImage {
		return CanvasNativeImage(BrowserImage.decodeToCanvas(data))
	}

	@Suppress("unused")
	object BrowserImage {
		suspend fun decodeToCanvas(bytes: ByteArray): JsDynamic? = korioSuspendCoroutine { c ->
			val blob = jsNew("Blob", jsArray(bytes), jsObject("type" to "image/png"))
			val blobURL = global["URL"].call("createObjectURL", blob)

			val img = jsNew("Image")
			img["onload"] = jsFunctionRaw0 {
				val canvas = document.call("createElement", "canvas")
				canvas["width"] = img["width"]
				canvas["height"] = img["height"]
				val ctx = canvas.call("getContext", "2d")
				ctx.call("drawImage", img, 0, 0)
				global["URL"].call("revokeObjectURL", blobURL)
				c.resume(canvas)
			}
			img["onerror"] = jsFunctionRaw0 {
				c.resumeWithException(RuntimeException("error loading image"))
			}
			img["src"] = blobURL
		}

		fun imgData(canvas: JsDynamic?, out: IntArray): Unit {
			HtmlImage.renderHtmlCanvasIntoBitmap(canvas, out)
		}
	}
}

class CanvasContext2d(canvas: JsDynamic?) : Context2d.Renderer() {
	val ctx = canvas.call("getContext", "2d")

	fun Context2d.Paint.toJsStr(): Any? {
		return when (this) {
			is Context2d.None -> "none"
			is Context2d.Color -> NamedColors.toHtmlString(this.color)
			is Context2d.LinearGradient -> {
				val grad = ctx.call("createLinearGradient", this.x0, this.y0, this.x1, this.y1)
				for (n in 0 until this.stops.size) {
					val stop = this.stops[n]
					val color = this.colors[n]
					grad.call("addColorStop", stop, NamedColors.toHtmlString(color))
				}
				grad
			}
			is Context2d.RadialGradient -> {
				val grad = ctx.call("createRadialGradient", this.x0, this.y0, this.r0, this.x1, this.y1, this.r1)
				for (n in 0 until this.stops.size) {
					val stop = this.stops[n]
					val color = this.colors[n]
					grad.call("addColorStop", stop, NamedColors.toHtmlString(color))
				}
				grad
			}
			is Context2d.BitmapPaint -> {
				ctx.call("createPattern", this.bitmap.toHtmlNative().canvas, if (this.repeat) "repeat" else "no-repeat")
			}
			else -> "black"
		}
	}

	inline private fun <T> keep(callback: () -> T): T {
		ctx.call("save")
		try {
			return callback()
		} finally {
			ctx.call("restore")
		}
	}

	private fun setState(state: Context2d.State, fill: Boolean) {
		ctx["globalAlpha"] = state.globalAlpha
		val font = state.font
		ctx["font"] = "${font.size}px ${font.name}"
		val t = state.transform
		ctx.call("setTransform", t.a, t.b, t.c, t.d, t.tx, t.ty)
		if (fill) {
			ctx["fillStyle"] = state.fillStyle.toJsStr()
		} else {
			ctx["lineWidth"] = state.lineWidth
			ctx["lineJoin"] = when (state.lineJoin) {
				Context2d.LineJoin.BEVEL -> "bevel"
				Context2d.LineJoin.MITER -> "miter"
				Context2d.LineJoin.ROUND -> "round"
			}
			ctx["lineCap"] = when (state.lineCap) {
				Context2d.LineCap.BUTT -> "butt"
				Context2d.LineCap.ROUND -> "round"
				Context2d.LineCap.SQUARE -> "sqare"
			}
			ctx["strokeStyle"] = state.strokeStyle.toJsStr()
		}
	}

	override fun render(state: Context2d.State, fill: Boolean) {
		if (state.path.isEmpty()) return

		//println("beginPath")
		keep {
			setState(state, fill)
			ctx.call("beginPath")

			state.path.visitCmds(
				moveTo = { x, y -> ctx.call("moveTo", x, y) },
				lineTo = { x, y -> ctx.call("lineTo", x, y) },
				quadTo = { cx, cy, ax, ay -> ctx.call("quadraticCurveTo", cx, cy, ax, ay) },
				cubicTo = { cx1, cy1, cx2, cy2, ax, ay -> ctx.call("bezierCurveTo", cx1, cy1, cx2, cy2, ax, ay) },
				close = { ctx.call("closePath") }
			)

			if (fill) {
				ctx.call("fill")
				//println("fill: $s")
			} else {
				ctx.call("stroke")
				//println("stroke: $s")
			}
		}
	}

	override fun renderText(state: Context2d.State, font: Context2d.Font, text: String, x: Double, y: Double, fill: Boolean) {
		keep {
			setState(state, fill)

			ctx["textBaseline"] = when (state.verticalAlign) {
				Context2d.VerticalAlign.TOP -> "top"
				Context2d.VerticalAlign.MIDLE -> "middle"
				Context2d.VerticalAlign.BASELINE -> "alphabetic"
				Context2d.VerticalAlign.BOTTOM -> "bottom"
			}
			ctx["textAlign"] = when (state.horizontalAlign) {
				Context2d.HorizontalAlign.LEFT -> "left"
				Context2d.HorizontalAlign.CENTER -> "center"
				Context2d.HorizontalAlign.RIGHT -> "right"
			}

			if (fill) {
				ctx.call("fillText", text, x, y)
			} else {
				ctx.call("strokeText", text, x, y)
			}
		}
	}

	override fun getBounds(font: Context2d.Font, text: String, out: Context2d.TextMetrics) {
		keep {
			val metrics = ctx.call("measureText", text)
			val width = metrics["width"].toInt()
			out.bounds.setTo(0.toDouble(), 0.toDouble(), width.toDouble() + 2, font.size)
		}
	}
}