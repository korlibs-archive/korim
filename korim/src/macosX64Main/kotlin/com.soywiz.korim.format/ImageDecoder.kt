package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlinx.cinterop.*
import platform.AppKit.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.posix.*
import kotlin.native.concurrent.*

private val ImageIOWorker by lazy { Worker.start() }

actual val nativeImageFormatProvider: NativeImageFormatProvider = object : BaseNativeNativeImageFormatProvider() {
    override suspend fun decode(data: ByteArray): NativeImage {
        return wrapNative(ImageIOWorker.execute(TransferMode.SAFE, { if (data.isFrozen) data else data.copyOf().freeze() }, { data ->
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
                        8.convert(), 0.convert(), colorSpace, CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                    )
                    try {
                        val gctx = NSGraphicsContext.graphicsContextWithCGContext(ctx, flipped = false)
                        NSGraphicsContext.setCurrentContext(gctx)
                        try {
                            image.drawInRect(imageRect)
                            val bytesPerRow = CGBitmapContextGetBytesPerRow(ctx).toInt()
                            val width = CGBitmapContextGetWidth(ctx).toInt()
                            val height = CGBitmapContextGetHeight(ctx).toInt()
                            val pixels = CGBitmapContextGetData(ctx)?.reinterpret<IntVar>() ?: error("Can't get pixels!")
                            val out = IntArray(width * height)
                            out.usePinned { outPin ->
                                val outStart = outPin.addressOf(0)
                                for (n in 0 until height) {
                                    memcpy(outStart + width * n, pixels.reinterpret<ByteVar>() + bytesPerRow * n, (width * 4).convert())
                                }
                            }
                            Bitmap32(width, height, RgbaArray(out), premult = true)
                        } finally {
                            NSGraphicsContext.setCurrentContext(null)
                        }
                    } finally {
                        CGContextRelease(ctx)
                    }
                } finally {
                    CGColorSpaceRelease(colorSpace)
                }
            }
        }).await())
    }
}
