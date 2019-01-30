import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIGraphicsRendererContext

/*
package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import platform.posix.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import kotlin.native.concurrent.*

private val ImageIOWorker by lazy { Worker.start() }

actual val nativeImageFormatProvider: NativeImageFormatProvider = object : BaseNativeNativeImageFormatProvider() {
    override suspend fun decode(data: ByteArray): NativeImage {
        return wrapNative(ImageIOWorker.execute(TransferMode.SAFE, { if (data.isFrozen) data else data.copyOf().freeze() }, { data ->
            autoreleasepool {
                memScoped {
                    val nsdata: NSData = data.usePinned { dataPin ->
                        NSData.dataWithBytes(dataPin.addressOf(0), data.size.convert())
                    }

                    val image = UIImage.imageWithData(nsdata) ?: error("Can't read image")
                    val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB)
                    try {
                        val rwidth = image.size.useContents { this.width }
                        val rheight = image.size.useContents { this.height }
                        val width = rwidth.toInt()
                        val height = rheight.toInt()
                        val imageRect = alloc<CGRect>().apply {
                            //origin.x = 0.uncheckedCast()
                            //origin.y = 0.0
                            size.width = rwidth
                            size.height = rheight
                        }
                        println("UIImage.imageWithData: nsdata=${data.size}, width=$width, height=$height")
                        val point = alloc<CGPoint>()
                        val out = IntArray(width * height).apply {
                            usePinned { pin ->
                                val ctx = CGBitmapContextCreate(
                                    pin.addressOf(0),
                                    width.convert(),
                                    height.convert(),
                                    8.convert(),
                                    (width * 4).convert(),
                                    colorSpace,
                                    CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                                )
                                try {
                                    UIGraphicsPushContext(ctx)
                                    try {
                                        //image.drawInRect(imageRect.readValue())
                                        //CGContextDrawImage(ctx, imageRect.readValue(), image.CGImage)
                                        //CGContextFlush(ctx)

                                        CGContextSaveGState(ctx)
                                        CGContextTranslateCTM(ctx, 0.0, rheight)
                                        CGContextScaleCTM(ctx, 1.0, -1.0)
                                        CGContextDrawImage(ctx, imageRect.readValue(), image.CGImage)
                                        CGContextRestoreGState(ctx)
                                    } finally {
                                        UIGraphicsPopContext()
                                    }
                                } finally {
                                    CGContextRelease(ctx)
                                }
                            }
                        }

                        Bitmap32(width, height, RgbaArray(out), premultiplied = true).apply {
                            flipY()
                        }
                    } finally {
                        CGColorSpaceRelease(colorSpace)
                    }
                }
            }
        }).await())
    }
}
*/
