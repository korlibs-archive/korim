package com.soywiz.korim.format

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import kotlinx.cinterop.*
import platform.AppKit.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.ImageIO.*
import platform.posix.*
import kotlin.math.*
import kotlin.native.concurrent.*

private val ImageIOWorker by lazy { Worker.start() }

// https://developer.apple.com/library/archive/documentation/GraphicsImaging/Conceptual/drawingwithquartz2d/dq_context/dq_context.html#//apple_ref/doc/uid/TP30001066-CH203-BCIBHHBB
actual val nativeImageFormatProvider: NativeImageFormatProvider = object : BaseNativeNativeImageFormatProvider() {
    override fun createBitmapNativeImage(bmp: Bitmap) = MacosNativeImage(bmp.toBMP32().premultipliedIfRequired())

    override suspend fun decode(data: ByteArray, premultiplied: Boolean): NativeImage {
        data class Info(val data: ByteArray, val premultiplied: Boolean)
        return wrapNative(
            ImageIOWorker.execute(
                TransferMode.SAFE,
                { Info(if (data.isFrozen) data else data.copyOf().freeze(), premultiplied) },
                { info ->
                    val data = info.data
                    val premultiplied = info.premultiplied
                    autoreleasepool {
                        val nsdata: NSData = data.usePinned { dataPin ->
                            NSData.dataWithBytes(dataPin.addressOf(0), data.size.convert())
                        }

                        val image = NSImage(data = nsdata)
                        var iwidth = 0
                        var iheight = 0
                        val imageSize = image.size
                        imageSize.useContents { iwidth = width.toInt(); iheight = height.toInt() }
                        val imageRect = NSMakeRect(0.0, 0.0, iwidth.toDouble(), iheight.toDouble())
                        val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB)
                        try {
                            val ctx = CGBitmapContextCreate(
                                null, iwidth.convert(), iheight.convert(),
                                8.convert(), 0.convert(), colorSpace, when (premultiplied) {
                                    true -> CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                                    false -> CGImageAlphaInfo.kCGImageAlphaLast.value
                                }
                            )
                            try {
                                val oldContext = NSGraphicsContext.currentContext
                                val gctx = NSGraphicsContext.graphicsContextWithCGContext(ctx, flipped = false)
                                NSGraphicsContext.setCurrentContext(gctx)
                                try {
                                    image.drawInRect(imageRect)
                                    Bitmap32(iwidth, iheight, premultiplied = premultiplied).also { bmp ->
                                        transferBitmap32CGContext(bmp, ctx, toBitmap = true)
                                    }
                                } finally {
                                    NSGraphicsContext.setCurrentContext(oldContext)
                                }
                            } finally {
                                CGContextRelease(ctx)
                            }
                        } finally {
                            CGColorSpaceRelease(colorSpace)
                        }
                    }
                }).await(), premultiplied
        )
    }
}

private fun transferBitmap32CGContext(bmp: Bitmap32, ctx: CGContextRef?, toBitmap: Boolean) {
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

class MacosNativeImage(bitmap: Bitmap32) : BitmapNativeImage(bitmap) {
    override fun toNonNativeBmp(): Bitmap = bitmap.clone()

    override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(NativeRenderer(bitmap, antialiasing))
}

private inline fun <T> cgKeepState(ctx: CGContextRef?, callback: () -> T): T {
    CGContextSaveGState(ctx)
    try {
        return callback()
    } finally {
        CGContextRestoreGState(ctx)
    }
}

class NativeRenderer(val bmp: Bitmap32, val antialiasing: Boolean) : Context2d.BufferedRenderer() {
    override val width: Int get() = bmp.width
    override val height: Int get() = bmp.height

    fun Matrix.toCGAffineTransform() = CGAffineTransformMake(a, b, c, d, tx, ty)

    fun RGBA.toFloats(placement: NativePlacement): CArrayPointer<CGFloatVar> {
        val color = this
        return placement.allocArray<CGFloatVar>(4).also {
            it[0] = color.ad
            it[1] = color.bd
            it[2] = color.gd
            it[3] = color.rd
        }
    }

    override fun getBounds(font: Context2d.Font, text: String, out: Context2d.TextMetrics) {
        super.getBounds(font, text, out)
    }

    fun RGBA.toCG() = CGColorCreateGenericRGB(rd, gd, bd, ad)

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
                CGRectMake(0.0, 0.0, CGBitmapContextGetWidth(ctx).toInt().toDouble(), CGBitmapContextGetHeight(ctx).toInt().toDouble()),
                image
            )
        } else {
            CGContextDrawImage(
                ctx,
                CGRectMake(0.0, 0.0, bmp.width.toDouble(), bmp.height.toDouble()),
                image
            )
        }
        //println("MACOS: imageCtx=$imageCtx, image=$image")
        CGImageRelease(image)
        CGContextRelease(imageCtx)
    }

    override fun flushCommands() {
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
                                CGContextSetAlpha(ctx, state.globalAlpha)
                                CGContextConcatCTM(ctx, state.transform.toCGAffineTransform())
                                state.path.visitCmds(
                                    moveTo = { x, y -> CGContextMoveToPoint(ctx, x, y) },
                                    lineTo = { x, y -> CGContextAddLineToPoint(ctx, x, y) },
                                    quadTo = { cx, cy, ax, ay -> CGContextAddQuadCurveToPoint(ctx, cx, cy, ax, ay) },
                                    cubicTo = { cx1, cy1, cx2, cy2, ax, ay ->
                                        CGContextAddCurveToPoint(ctx, cx1, cy1, cx2, cy2, ax, ay)
                                    },
                                    close = { CGContextClosePath(ctx) }
                                )
                                memScoped {
                                    val style = if (fill) state.fillStyle else state.strokeStyle
                                    when (style) {
                                        is Context2d.None -> Unit
                                        is Context2d.Color -> {
                                            if (fill) {
                                                CGContextSetFillColorWithColor(ctx, style.color.toCG())
                                                CGContextFillPath(ctx)
                                            } else {
                                                CGContextSetStrokeColorWithColor(ctx, style.color.toCG())
                                                CGContextStrokePath(ctx)
                                            }
                                        }
                                        is Context2d.Gradient -> {
                                            if (fill) {
                                                val nelements = style.colors.size
                                                val colors = CFArrayCreate(null, null, 0, null)
                                                val locations = allocArray<CGFloatVar>(nelements)
                                                for (n in 0 until nelements) {
                                                    val color = RGBA(style.colors[n])
                                                    val stop = style.stops[n]
                                                    CFArrayAppendValue(colors, CGColorCreateGenericRGB(color.rd, color.gd, color.bd, color.ad))
                                                    locations[n] = stop
                                                }
                                                val options = kCGGradientDrawsBeforeStartLocation or kCGGradientDrawsAfterEndLocation

                                                CGContextClip(ctx)
                                                val gradient = CGGradientCreateWithColors(colorSpace, colors, locations)
                                                val start = CGPointMake(style.x0, style.y0)
                                                val end = CGPointMake(style.x1, style.y1)
                                                when (style.kind) {
                                                    Context2d.Gradient.Kind.LINEAR -> {
                                                        CGContextDrawLinearGradient(ctx, gradient, start, end, options)
                                                    }
                                                    Context2d.Gradient.Kind.RADIAL -> {
                                                        CGContextDrawRadialGradient(
                                                            ctx, gradient, start, style.r0, end, style.r1, options
                                                        )
                                                        CGGradientRelease(gradient)
                                                    }
                                                }
                                                CGGradientRelease(gradient)
                                            }
                                        }
                                        is Context2d.BitmapPaint -> {
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

inline fun Number.toCgFloat(): CGFloat = this.toDouble()
