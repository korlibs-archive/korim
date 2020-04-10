package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.internal.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.TextMetrics
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import com.soywiz.korio.util.encoding.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlinx.coroutines.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.url.*
import org.w3c.files.*
import kotlin.browser.*
import kotlin.coroutines.*
import kotlin.js.*
import kotlin.math.*

actual val nativeImageFormatProvider: NativeImageFormatProvider = HtmlNativeImageFormatProvider

open class HtmlNativeImage(val texSource: TexImageSource, width: Int, height: Int) :
	NativeImage(width, height, texSource, true) {
	override val name: String = "HtmlNativeImage"
	val element: HTMLElement get() = texSource as HTMLElement

	constructor(img: HTMLImageElementLike) : this(img, img.width, img.height)
	constructor(canvas: HTMLCanvasElementLike) : this(canvas, canvas.width, canvas.height)

	val lazyCanvasElement: HTMLCanvasElementLike by lazy {
        if (texSource.asDynamic().src !== undefined) {
            BrowserImage.imageToCanvas(texSource.unsafeCast<HTMLImageElementLike>())
        } else {
            texSource.unsafeCast<HTMLCanvasElementLike>()
        }
	}

	override fun toNonNativeBmp(): Bitmap {
		val data = RgbaArray(width * height)
		HtmlImage.renderHtmlCanvasIntoBitmap(lazyCanvasElement, data)
		return Bitmap32(width, height, data)
	}

	override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(CanvasContext2dRenderer(lazyCanvasElement))
}

object HtmlNativeImageFormatProvider : NativeImageFormatProvider() {
	override suspend fun decode(data: ByteArray, premultiplied: Boolean): NativeImage {
		return HtmlNativeImage(BrowserImage.decodeToCanvas(data, premultiplied))
	}

	override suspend fun decode(vfs: Vfs, path: String, premultiplied: Boolean): NativeImage {
		//println("HtmlNativeImageFormatProvider.decode($vfs, '$path')")
		return when (vfs) {
			is LocalVfs -> {
				//println("LOCAL: HtmlNativeImageFormatProvider: $vfs, $path")
				HtmlNativeImage(BrowserImage.loadImage(path))
			}
			is UrlVfs -> {
				val jsUrl = vfs.getFullUrl(path)
				//println("URL: HtmlNativeImageFormatProvider: $vfs, $path : $jsUrl")
				HtmlNativeImage(BrowserImage.loadImage(jsUrl, premultiplied))
			}
			else -> {
				//println("OTHER: HtmlNativeImageFormatProvider: $vfs, $path")
				HtmlNativeImage(BrowserImage.decodeToCanvas(vfs[path].readAll(), premultiplied))
			}
		}
	}

	override fun create(width: Int, height: Int): NativeImage {
		return HtmlNativeImage(HtmlCanvas.createCanvas(width, height))
	}

	override fun copy(bmp: Bitmap): NativeImage {
		return HtmlNativeImage(HtmlImage.bitmapToHtmlCanvas(bmp.toBMP32()))
	}

	override suspend fun display(bitmap: Bitmap, kind: Int) {
		if (kind == 1) {
			val img = document.createElement("img")
			img.setAttribute("src", "data:image/png;base64," + PNG.encode(bitmap).toBase64())
			document.body?.appendChild(img)
		} else {
			val img = bitmap.toHtmlNative()
			document.body?.appendChild(img.element)
		}
	}

	override fun mipmap(bmp: Bitmap, levels: Int): NativeImage {
		var out = bmp.ensureNative()
		for (n in 0 until levels) out = mipmap(out)
		return out
	}

	override fun mipmap(bmp: Bitmap): NativeImage {
		val out = NativeImage(ceil(bmp.width * 0.5).toInt(), ceil(bmp.height * 0.5).toInt())
		out.getContext2d(antialiasing = true).renderer.drawImage(bmp, 0, 0, out.width, out.height)
		return out
	}
}

@Suppress("unused")
object BrowserImage {
    private fun toNodeJsBuffer(@Suppress("UNUSED_PARAMETER") ba: ByteArray): dynamic = js("(Buffer.from(ba.buffer))")

	suspend fun decodeToCanvas(bytes: ByteArray, premultiplied: Boolean = true): HTMLCanvasElementLike {
        if (OS.isJsNodeJs) {
            val canvas = nodeJsCanvas ?: error("Canvas not available")
            return (canvas.loadImage(toNodeJsBuffer(bytes)) as Promise<HTMLCanvasElementLike>).await()
        } else {
            val blob = Blob(arrayOf(bytes), BlobPropertyBag(type = "image/png"))
            val blobURL = URL.createObjectURL(blob)
            try {
                return loadCanvas(blobURL)
            } finally {
                URL.revokeObjectURL(blobURL)
            }
        }
	}

	fun imageToCanvas(img: HTMLImageElementLike): HTMLCanvasElementLike {
        val canvas = HtmlCanvas.createCanvas(img.width, img.height)
        //println("[onload.b]")
        val ctx: CanvasRenderingContext2D = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
        //println("[onload.c]")
        ctx.drawImage(img.unsafeCast<CanvasImageSource>(), 0.0, 0.0)
        return canvas
	}

	suspend fun loadImage(jsUrl: String, premultiplied: Boolean = true): HTMLImageElementLike = suspendCancellableCoroutine { c ->
		// Doesn't work with Kotlin.JS
		//val img = document.createElement("img") as HTMLImageElement
		//println("[1]")
		if (OS.isJsNodeJs) {
            val canvas = nodeJsCanvas ?: error("Canvas not available")
            (canvas.loadImage(jsUrl) as Promise<HTMLImageElementLike>).then({ v ->
				c.resume(v)
			}, { v ->
				c.resumeWithException(v)
			})
			Unit
		} else {
			val img = document.createElement("img").unsafeCast<HTMLImageElement>()
			img.onload = {
				c.resume(img.unsafeCast<HTMLImageElementLike>())
			}
			img.onerror = { _, _, _, _, _ ->
				c.resumeWithException(RuntimeException("error loading image $jsUrl"))
			}
			img.src = jsUrl
			Unit
		}
	}

	suspend fun loadCanvas(jsUrl: String): HTMLCanvasElementLike {
		return imageToCanvas(loadImage(jsUrl))
	}
}

class CanvasContext2dRenderer(private val canvas: HTMLCanvasElementLike) : Context2d.Renderer() {
	override val width: Int get() = canvas.width.toInt()
	override val height: Int get() = canvas.height.toInt()

	val ctx = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()

	fun Context2d.Paint.toJsStr(): Any? {
		return when (this) {
			is Context2d.None -> "none"
			is Context2d.Color -> this.color.htmlStringSimple
			is Context2d.Gradient -> {
				when (kind) {
					Context2d.Gradient.Kind.LINEAR -> {
						val grad = ctx.createLinearGradient(this.x0, this.y0, this.x1, this.y1)
						for (n in 0 until this.stops.size) {
							val stop = this.stops[n]
							val color = this.colors[n]
							grad.addColorStop(stop, RGBA(color).htmlStringSimple)
						}
						grad
					}
					Context2d.Gradient.Kind.RADIAL -> {
						val grad = ctx.createRadialGradient(this.x0, this.y0, this.r0, this.x1, this.y1, this.r1)
						for (n in 0 until this.stops.size) {
							val stop = this.stops[n]
							val color = this.colors[n]
							grad.addColorStop(stop, RGBA(color).htmlStringSimple)
						}
						grad
					}
				}
			}
			is Context2d.BitmapPaint -> {
				ctx.createPattern(this.bitmap.toHtmlNative().texSource.unsafeCast<CanvasImageSource>(), if (this.repeat) "repeat" else "no-repeat")
				//ctx.call("createPattern", this.bitmap.toHtmlNative().canvas)
			}
			else -> "black"
		}
	}

	inline private fun <T> keep(callback: () -> T): T {
		ctx.save()
		try {
			return callback()
		} finally {
			ctx.restore()
		}
	}

	private fun setFont(font: Font) {
		ctx.font = "${font.size}px '${font.name}'"
	}

	private fun setState(state: Context2d.State, fill: Boolean) {
		ctx.globalAlpha = state.globalAlpha
		setFont(state.font)
		val t = state.transform
		ctx.setTransform(t.a, t.b, t.c, t.d, t.tx, t.ty)
		if (fill) {
			ctx.fillStyle = state.fillStyle.toJsStr()
		} else {
			ctx.lineWidth = state.lineWidth
			ctx.lineJoin = when (state.lineJoin) {
				LineJoin.SQUARE -> CanvasLineJoin.BEVEL
				LineJoin.MITER -> CanvasLineJoin.MITER
				LineJoin.ROUND -> CanvasLineJoin.ROUND
			}
			ctx.lineCap = when (state.lineCap) {
				LineCap.BUTT -> CanvasLineCap.BUTT
				LineCap.ROUND -> CanvasLineCap.ROUND
				LineCap.SQUARE -> CanvasLineCap.SQUARE
			}
			ctx.strokeStyle = state.strokeStyle.toJsStr()
		}
	}

	private fun transformPaint(paint: Context2d.Paint) {
		if (paint is Context2d.TransformedPaint) {
			val m = paint.transform
			ctx.transform(m.a, m.b, m.c, m.d, m.tx, m.ty)
		}
	}

	override fun drawImage(image: Bitmap, x: Int, y: Int, width: Int, height: Int, transform: Matrix) {
		ctx.save()
		try {
			transform.run { ctx.setTransform(a, b, c, d, tx, ty) }
			ctx.drawImage(
				(image.ensureNative() as HtmlNativeImage).texSource.unsafeCast<CanvasImageSource>(),
				x.toDouble(),
				y.toDouble(),
				width.toDouble(),
				height.toDouble()
			)
		} finally {
			ctx.restore()
		}
	}

	override fun render(state: Context2d.State, fill: Boolean) {
		if (state.path.isEmpty()) return

		//println("beginPath")
		keep {
			setState(state, fill)
			ctx.beginPath()

			state.path.visitCmds(
				moveTo = { x, y -> ctx.moveTo(x, y) },
				lineTo = { x, y -> ctx.lineTo(x, y) },
				quadTo = { cx, cy, ax, ay -> ctx.quadraticCurveTo(cx, cy, ax, ay) },
				cubicTo = { cx1, cy1, cx2, cy2, ax, ay -> ctx.bezierCurveTo(cx1, cy1, cx2, cy2, ax, ay) },
				close = { ctx.closePath() }
			)

			ctx.save()

			if (fill) {
				transformPaint(state.fillStyle)
				ctx.fill()
				//println("fill: $s")
			} else {
				transformPaint(state.strokeStyle)

				ctx.stroke()
				//println("stroke: $s")
			}

			ctx.restore()
		}
	}

	override fun renderText(
		state: Context2d.State,
		font: Font,
		text: String,
		x: Double,
		y: Double,
		fill: Boolean
	) {
		keep {
			setState(state, fill)

			ctx.textBaseline = when (state.verticalAlign) {
				VerticalAlign.TOP -> CanvasTextBaseline.TOP
				VerticalAlign.MIDDLE -> CanvasTextBaseline.MIDDLE
				VerticalAlign.BASELINE -> CanvasTextBaseline.ALPHABETIC
				VerticalAlign.BOTTOM -> CanvasTextBaseline.BOTTOM
                else -> CanvasTextBaseline.TOP
			}
			ctx.textAlign = when (state.horizontalAlign) {
				HorizontalAlign.LEFT -> CanvasTextAlign.LEFT
				HorizontalAlign.CENTER -> CanvasTextAlign.CENTER
				HorizontalAlign.RIGHT -> CanvasTextAlign.RIGHT
                HorizontalAlign.JUSTIFY -> CanvasTextAlign.LEFT
                else -> CanvasTextAlign.LEFT
			}

			if (fill) {
				ctx.fillText(text, x, y)
			} else {
				ctx.strokeText(text, x, y)
			}
		}
	}

	override fun getBounds(font: Font, text: String, out: TextMetrics) {
		keep {
			setFont(font)
			val metrics = ctx.measureText(text)
			val width = metrics.width.toInt()
			out.bounds.setTo(0.toDouble(), 0.toDouble(), width.toDouble() + 2, font.size)
		}
	}
}
