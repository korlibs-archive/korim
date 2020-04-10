@file:Suppress("UnusedImport")

package com.soywiz.korim.format.cg

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.posix.*
import kotlin.math.*

fun transferBitmap32CGContext(bmp: Bitmap32, ctx: CGContextRef?, toBitmap: Boolean) {
    val ctxBytesPerRow = CGBitmapContextGetBytesPerRow(ctx).toInt()
    val ctxWidth = CGBitmapContextGetWidth(ctx).toInt()
    val ctxHeight = CGBitmapContextGetHeight(ctx).toInt()
    val pixels = CGBitmapContextGetData(ctx)?.reinterpret<IntVar>() ?: error("Can't get pixels!")
    val minWidth = min(ctxWidth, bmp.width)
    val minHeight = min(ctxHeight, bmp.height)
    val out = bmp.data.ints
    out.usePinned { outPin ->
        val outStart = outPin.addressOf(0)
        val widthInBytes: size_t = (minWidth * 4).convert()
        for (n in 0 until minHeight) {
            val bmpPtr = outStart + ctxWidth * n
            val ctxPtr = pixels.reinterpret<ByteVar>() + ctxBytesPerRow * n
            when {
                toBitmap -> memcpy(bmpPtr, ctxPtr, widthInBytes)
                else -> memcpy(ctxPtr, bmpPtr, widthInBytes)
            }
        }
    }
}

class CoreGraphicsNativeImage(bitmap: Bitmap32) : BitmapNativeImage(bitmap) {
    override fun toNonNativeBmp(): Bitmap = bitmap.clone()

    override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(CoreGraphicsRenderer(bitmap, antialiasing))
}

private inline fun <T> cgKeepState(ctx: CGContextRef?, callback: () -> T): T {
    CGContextSaveGState(ctx)
    try {
        return callback()
    } finally {
        CGContextRestoreGState(ctx)
    }
}

class CoreGraphicsRenderer(val bmp: Bitmap32, val antialiasing: Boolean) : com.soywiz.korim.vector.renderer.BufferedRenderer() {
    override val width: Int get() = bmp.width
    override val height: Int get() = bmp.height

    fun Matrix.toCGAffineTransform() = CGAffineTransformMake(a.cg, b.cg, c.cg, d.cg, tx.cg, ty.cg)

    override fun getBounds(font: Font, fontSize: Double, text: String, out: TextMetrics) {
        super.getBounds(font, fontSize, text, out)
    }

    private fun cgDrawBitmap(bmp: Bitmap32, ctx: CGContextRef?, colorSpace: CPointer<CGColorSpace>?, tiled: Boolean = false) {
        val imageCtx = CGBitmapContextCreate(
            null, bmp.width.convert(), bmp.height.convert(),
            8.convert(), 0.convert(), colorSpace,
            CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
        )
        transferBitmap32CGContext(bmp, imageCtx, toBitmap = false)
        val image = CGBitmapContextCreateImage(imageCtx)


        if (tiled) {
            CGContextDrawTiledImage(
                ctx,
                CGRectMake(0.cg, 0.cg, CGBitmapContextGetWidth(ctx).toInt().cg, CGBitmapContextGetHeight(ctx).toInt().cg),
                image
            )
        } else {
            CGContextDrawImage(
                ctx,
                CGRectMake(0.cg, 0.cg, bmp.width.cg, bmp.height.cg),
                image
            )
        }
        //println("MACOS: imageCtx=$imageCtx, image=$image")
        CGImageRelease(image)
        CGContextRelease(imageCtx)
    }

    override fun flushCommands() {
        if (bmp.data.size == 0) return
        Releases { releases ->
            autoreleasepool {
                bmp.data.ints.usePinned { dataPin ->
                    //val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB)
                    val colorSpace = CGColorSpaceCreateDeviceRGB()
                    try {
                        val ctx = CGBitmapContextCreate(
                            dataPin.addressOf(0), bmp.width.convert(), bmp.height.convert(),
                            8.convert(), (bmp.width * 4).convert(), colorSpace,
                            CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                        )

                        //transferBitmap32CGContext(bmp, ctx, toBitmap = false)

                        try {
                            // Restore context
                            //cgKeepState(ctx) {
                            //    cgDrawBitmap(bmp, ctx, colorSpace)
                            //}

                            // @TODO: Check if command is a text command
                            for (command in commands) {
                                val state = command.state
                                val fill = command.fill

                                cgKeepState(ctx) {
                                    CGContextSetAllowsAntialiasing(ctx, antialiasing)
                                    CGContextSetAlpha(ctx, state.globalAlpha.cg)
                                    //CGContextConcatCTM(ctx, state.transform.toCGAffineTransform()) // Points already transformed
                                    state.path.visitCmds(
                                        moveTo = { x, y -> CGContextMoveToPoint(ctx, x.cg, y.cg) },
                                        lineTo = { x, y -> CGContextAddLineToPoint(ctx, x.cg, y.cg) },
                                        quadTo = { cx, cy, ax, ay -> CGContextAddQuadCurveToPoint(ctx, cx.cg, cy.cg, ax.cg, ay.cg) },
                                        cubicTo = { cx1, cy1, cx2, cy2, ax, ay -> CGContextAddCurveToPoint(ctx, cx1.cg, cy1.cg, cx2.cg, cy2.cg, ax.cg, ay.cg) },
                                        close = { CGContextClosePath(ctx) }
                                    )
                                    if (!fill) {
                                        CGContextSetLineWidth(ctx, state.lineWidth.cg)
                                        CGContextSetMiterLimit(ctx, state.miterLimit.cg)
                                        CGContextSetLineJoin(
                                            ctx, when (state.lineJoin) {
                                                LineJoin.BEVEL -> CGLineJoin.kCGLineJoinBevel
                                                LineJoin.MITER -> CGLineJoin.kCGLineJoinMiter
                                                LineJoin.ROUND -> CGLineJoin.kCGLineJoinRound
                                            }
                                        )
                                        CGContextSetLineCap(
                                            ctx, when (state.lineCap) {
                                                LineCap.BUTT -> CGLineCap.kCGLineCapButt
                                                LineCap.ROUND -> CGLineCap.kCGLineCapRound
                                                LineCap.SQUARE -> CGLineCap.kCGLineCapSquare
                                            }
                                        )
                                    }
                                    memScoped {
                                        val style = if (fill) state.fillStyle else state.strokeStyle
                                        when (style) {
                                            is NonePaint -> Unit
                                            is ColorPaint -> {
                                                if (fill) {
                                                    CGContextSetFillColorWithColor(
                                                        ctx,
                                                        style.color.toCgColor(releases, colorSpace)
                                                    )
                                                    CGContextFillPath(ctx)
                                                } else {
                                                    CGContextSetStrokeColorWithColor(
                                                        ctx,
                                                        style.color.toCgColor(releases, colorSpace)
                                                    )
                                                    CGContextStrokePath(ctx)
                                                }
                                            }
                                            is GradientPaint -> {
                                                if (fill) {
                                                    val nelements = style.colors.size
                                                    val colors = CFArrayCreate(null, null, 0, null)
                                                    val locations = allocArray<CGFloatVar>(nelements)
                                                    for (n in 0 until nelements) {
                                                        val color = RGBA(style.colors[n])
                                                        val stop = style.stops[n]
                                                        CFArrayAppendValue(colors, color.toCgColor(releases, colorSpace))
                                                        locations[n] = stop.cg
                                                    }
                                                    val options =
                                                        kCGGradientDrawsBeforeStartLocation or kCGGradientDrawsAfterEndLocation

                                                    CGContextClip(ctx)
                                                    val m = state.transform
                                                    val gradient = CGGradientCreateWithColors(colorSpace, colors, locations)
                                                    val start = CGPointMake(style.x0(m).cg, style.y0(m).cg)
                                                    val end = CGPointMake(style.x1(m).cg, style.y1(m).cg)
                                                    when (style.kind) {
                                                        GradientKind.LINEAR -> {
                                                            CGContextDrawLinearGradient(ctx, gradient, start, end, options)
                                                        }
                                                        GradientKind.RADIAL -> {
                                                            CGContextDrawRadialGradient(ctx, gradient, start, style.r0(m).cg, end, style.r1(m).cg, options)
                                                            CGGradientRelease(gradient)
                                                        }
                                                    }
                                                    CGGradientRelease(gradient)
                                                }
                                            }
                                            is BitmapPaint -> {
                                                CGContextClip(ctx)
                                                cgKeepState(ctx) {
                                                    CGContextConcatCTM(ctx, state.transform.toCGAffineTransform())
                                                    cgDrawBitmap(bmp, ctx, colorSpace, tiled = style.repeat)
                                                }
                                                //println("Not implemented style $style fill=$fill")
                                            }
                                            else -> {
                                                println("Not implemented style $style fill=$fill")
                                            }
                                        }
                                    }
                                }
                            }

                        } finally {
                            //transferBitmap32CGContext(bmp, ctx, toBitmap = true)
                            CGContextRelease(ctx)
                        }
                    } finally {
                        CGColorSpaceRelease(colorSpace)
                    }
                }
            }
        }
    }
}

//fun RGBA.toCG() = CGColorCreateGenericRGB(rd, gd, bd, ad)

internal class Releases {
    val colors = arrayListOf<CGColorRef?>()

    companion object {
        inline operator fun invoke(callback: (Releases) -> Unit) {
            val releases = Releases()
            try {
                callback(releases)
            } finally {
                releases.release()
            }
        }
    }

    fun release() {
        for (color in colors) {
            CGColorRelease(color)
        }
    }
}

internal fun RGBA.toCgColor(releases: Releases, space: CGColorSpaceRef?) = memScoped {
    // Not available on iOS
    //CGColorCreateGenericRGB(color.rd.cg, color.gd.cg, color.bd.cg, color.ad.cg)
    val data = allocArray<CGFloatVar>(4)
    data[0] = this@toCgColor.rd.cg
    data[1] = this@toCgColor.gd.cg
    data[2] = this@toCgColor.bd.cg
    data[3] = this@toCgColor.ad.cg
    val color = CGColorCreate(space, data)
    releases.colors.add(color)
    color
}
