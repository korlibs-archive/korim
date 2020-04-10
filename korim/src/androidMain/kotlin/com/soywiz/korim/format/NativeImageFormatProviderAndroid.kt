package com.soywiz.korim.format

import android.app.*
import android.graphics.*
import android.text.*
import android.view.*
import android.widget.*
import com.soywiz.korio.android.androidContext
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.*
import com.soywiz.korim.font.Font
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.vector.*
import kotlinx.coroutines.*

actual val nativeImageFormatProvider: NativeImageFormatProvider = AndroidNativeImageFormatProvider

object AndroidNativeImageFormatProvider : NativeImageFormatProvider() {
    override suspend fun display(bitmap: Bitmap, kind: Int) {
        val ctx = androidContext()
        val androidBitmap = bitmap.toAndroidBitmap()
        val deferred = CompletableDeferred<Unit>()
        (ctx as Activity).runOnUiThread {
            val settingsDialog = Dialog(ctx)
            settingsDialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
            val rlmain = LinearLayout(ctx)
            val llp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            val ll1 = LinearLayout(ctx)

            val iv = ImageView(ctx)
            iv.setBackgroundColor(Colors.BLACK.value)
            iv.setImageBitmap(androidBitmap)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            iv.layoutParams = lp

            ll1.addView(iv)
            rlmain.addView(ll1)
            settingsDialog.setContentView(rlmain, llp)

            settingsDialog.setOnDismissListener {
                deferred.complete(Unit)
            }
            settingsDialog.show()
        }
        deferred.await()
    }

    override suspend fun decode(data: ByteArray, premultiplied: Boolean): NativeImage =
		AndroidNativeImage(BitmapFactory.decodeByteArray(data, 0, data.size, BitmapFactory.Options().apply { this.inPremultiplied = premultiplied }))

	override fun create(width: Int, height: Int): NativeImage {
		val bmp = android.graphics.Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
		//bmp.setPixels()
		return AndroidNativeImage(bmp)
	}

	override fun copy(bmp: Bitmap): NativeImage = AndroidNativeImage(bmp.toAndroidBitmap())
}

/*
suspend fun androidQuestionAlert(message: String, title: String = "Warning"): Boolean = korioSuspendCoroutine { c ->
	KorioAndroidContext.runOnUiThread {
		val dialog = AlertDialog.Builder(KorioAndroidContext)
			.setTitle(title)
			.setMessage(message)
			.setPositiveButton(android.R.string.yes) { dialog, which ->
				c.resume(true)
			}
			.setNegativeButton(android.R.string.no, android.content.DialogInterface.OnClickListener { dialog, which ->
				c.resume(false)
			})
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(false)
			.show()

		dialog.show()
	}

 */

fun Bitmap.toAndroidBitmap(): android.graphics.Bitmap {
    if (this is AndroidNativeImage) return this.androidBitmap
    val bmp32 = this.toBMP32()
    return android.graphics.Bitmap.createBitmap(
        bmp32.data.ints,
        0,
        bmp32.width,
        bmp32.width,
        bmp32.height,
        android.graphics.Bitmap.Config.ARGB_8888
    )
}

class AndroidNativeImage(val androidBitmap: android.graphics.Bitmap) :
    NativeImage(androidBitmap.width, androidBitmap.height, androidBitmap, premultiplied = androidBitmap.isPremultiplied()) {
    override val name: String = "AndroidNativeImage"

    override fun toNonNativeBmp(): Bitmap {
        val out = RgbaArray(width * height)
        androidBitmap.getPixels(out.ints, 0, width, 0, 0, width, height)
        return Bitmap32(width, height, out, premultiplied = premultiplied)
    }

    override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(AndroidContext2dRenderer(androidBitmap))
}

class AndroidContext2dRenderer(val bmp: android.graphics.Bitmap) : Context2d.Renderer() {
    override val width: Int get() = bmp.width
    override val height: Int get() = bmp.height
    //val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
    val paint =
        Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG or TextPaint.ANTI_ALIAS_FLAG or TextPaint.SUBPIXEL_TEXT_FLAG).apply {
            hinting = Paint.HINTING_ON
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
    val canvas = Canvas(bmp)
    val matrixValues = FloatArray(9)
    var androidMatrix = android.graphics.Matrix()

    fun GraphicsPath.toAndroid(): Path {
        val out = Path()

        out.fillType = when (this.winding) {
            Winding.EVEN_ODD -> Path.FillType.EVEN_ODD
            Winding.NON_ZERO -> Path.FillType.INVERSE_EVEN_ODD
            else -> Path.FillType.EVEN_ODD
        }
        //kotlin.io.println("Path:")
        this.visitCmds(
            moveTo = { x, y -> out.moveTo(x.toFloat(), y.toFloat()) },
            lineTo = { x, y -> out.lineTo(x.toFloat(), y.toFloat()) },
            quadTo = { cx, cy, ax, ay -> out.quadTo(cx.toFloat(), cy.toFloat(), ax.toFloat(), ay.toFloat()) },
            cubicTo = { cx1, cy1, cx2, cy2, ax, ay ->
                out.cubicTo(
                    cx1.toFloat(),
                    cy1.toFloat(),
                    cx2.toFloat(),
                    cy2.toFloat(),
                    ax.toFloat(),
                    ay.toFloat()
                )
            },
            close = { out.close() }
        )
        //kotlin.io.println("/Path")
        return out
    }

    fun convertPaint(c: Paint, m: com.soywiz.korma.geom.Matrix, out: Paint) {
        when (c) {
            is Context2d.None -> {
                out.shader = null
            }
            is ColorPaint -> {
                out.color = BGRA.packRGBA(c.color)
                out.shader = null
            }
            is GradientPaint -> {
                when (c.kind) {
                    GradientPaint.Kind.LINEAR ->
                        out.shader = LinearGradient(
                            c.x0(m).toFloat(), c.y0(m).toFloat(),
                            c.x1(m).toFloat(), c.y1(m).toFloat(),
                            c.colors.toIntArray(), c.stops.map(Double::toFloat).toFloatArray(), Shader.TileMode.CLAMP
                        )
                    GradientPaint.Kind.RADIAL ->
                        out.shader = RadialGradient(
                            c.x1(m).toFloat(), c.y1(m).toFloat(), c.r1(m).toFloat(),
                            c.colors.toIntArray(), c.stops.map(Double::toFloat).toFloatArray(), Shader.TileMode.CLAMP
                        )
                }

            }
        }
    }

    inline fun <T> keep(callback: () -> T): T {
        canvas.save()
        try {
            return callback()
        } finally {
            canvas.restore()
        }
    }

    fun android.graphics.Matrix.setTo(m: com.soywiz.korma.geom.Matrix) = this.apply {
        matrixValues[Matrix.MSCALE_X] = m.a.toFloat()
        matrixValues[Matrix.MSKEW_X] = m.b.toFloat()
        matrixValues[Matrix.MSKEW_Y] = m.c.toFloat()
        matrixValues[Matrix.MSCALE_Y] = m.d.toFloat()
        matrixValues[Matrix.MTRANS_X] = m.tx.toFloat()
        matrixValues[Matrix.MTRANS_Y] = m.ty.toFloat()
        matrixValues[Matrix.MPERSP_0] = 0f
        matrixValues[Matrix.MPERSP_1] = 0f
        matrixValues[Matrix.MPERSP_2] = 1f
        this.setValues(matrixValues)
    }

    private fun setState(state: Context2d.State, fill: Boolean) {
        //canvas.matrix = androidMatrix.setTo(state.transform)
        paint.strokeWidth = state.lineWidth.toFloat()
    }

    override fun render(state: Context2d.State, fill: Boolean) {
        setState(state, fill)

        keep {
            if (state.clip != null) canvas.clipPath(state.clip?.toAndroid())

            if (fill) {
                paint.style = android.graphics.Paint.Style.FILL
                convertPaint(state.fillStyle, state.transform, paint)
            } else {
                paint.style = android.graphics.Paint.Style.STROKE
                convertPaint(state.strokeStyle, state.transform, paint)
            }

            //println("-----------------")
            //println(canvas.matrix)
            //println(state.path.toAndroid())
            //println(paint.style)
            //println(paint.color)
            canvas.drawPath(state.path.toAndroid(), paint)
        }
    }

    override fun renderText(
        state: Context2d.State,
        font: Font,
        fontSize: Double,
        text: String,
        x: Double,
        y: Double,
        fill: Boolean
    ) {
        val metrics = TextMetrics()
        val bounds = metrics.bounds
        paint.typeface = Typeface.create(font.name, Typeface.NORMAL)
        paint.textSize = fontSize.toFloat()
        val fm = paint.fontMetrics
        getBounds(font, fontSize, text, metrics)

        val baseline = fm.ascent + fm.descent

        val ox = state.horizontalAlign.getOffsetX(bounds.width)
        val oy = state.verticalAlign.getOffsetY(bounds.height, baseline.toDouble())

        //val tp = TextPaint(TextPaint.ANTI_ALIAS_FLAG)

        canvas.drawText(text, 0, text.length, (x - ox).toFloat(), (y + baseline - oy).toFloat(), paint)
    }

    override fun getBounds(font: Font, size: Double, text: String, out: TextMetrics) {
        val rect = Rect()
        paint.getTextBounds(text, 0, text.length, rect)
        out.bounds.setTo(rect.left.toDouble(), rect.top.toDouble(), rect.width().toDouble(), rect.height().toDouble())
    }
}

