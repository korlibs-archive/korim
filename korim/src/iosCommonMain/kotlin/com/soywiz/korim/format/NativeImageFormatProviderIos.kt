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
import com.soywiz.korim.format.cg.*
import kotlin.native.concurrent.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korio.util.encoding.*

private val ImageIOWorker by lazy { Worker.start() }

actual val nativeImageFormatProvider: NativeImageFormatProvider = object : BaseNativeNativeImageFormatProvider() {
    override suspend fun decode(data: ByteArray, premultiplied: Boolean): NativeImage {
        data class Info(val data: ByteArray, val premultiplied: Boolean)

        return wrapNative(ImageIOWorker.execute(TransferMode.SAFE, { Info(if (data.isFrozen) data else data.copyOf().freeze(), premultiplied) }, { info ->
            val data = info.data
            val premultiplied = info.premultiplied
            autoreleasepool {
                memScoped {
                    val nsdata: NSData = data.usePinned { dataPin ->
                        NSData.dataWithBytes(dataPin.addressOf(0), data.size.convert())
                    }

                    val image = UIImage.imageWithData(nsdata) ?: error("Can't read image")
                    val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB)
                    try {
                        val width = image.size.useContents { this.width }.toInt()
                        val height = image.size.useContents { this.height }.toInt()
                        //println("UIImage.imageWithData: nsdata=${data.size}, width=$width, height=$height")
                        val out = IntArray(width * height).apply {
                            usePinned { pin ->
                                val ctx = CGBitmapContextCreate(
                                    pin.addressOf(0),
                                    width.convert(),
                                    height.convert(),
                                    8.convert(),
                                    (width * 4).convert(),
                                    colorSpace,
                                    when (premultiplied) {
                                        true -> CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                                        false -> CGImageAlphaInfo.kCGImageAlphaLast.value
                                    }

                                )
                                try {
                                    UIGraphicsPushContext(ctx)
                                    try {
                                        //CGContextSaveGState(ctx)
                                        //CGContextTranslateCTM(ctx, 0.0.toCgFloat(), height.toCgFloat())
                                        //CGContextScaleCTM(ctx, 1.0.toCgFloat(), (-1.0).toCgFloat())
                                        CGContextDrawImage(ctx, CGRectMake(0.0.toCgFloat(), 0.0.toCgFloat(), width.toCgFloat(), height.toCgFloat()), image.CGImage)
                                        //CGContextRestoreGState(ctx)
                                    } finally {
                                        UIGraphicsPopContext()
                                    }
                                } finally {
                                    CGContextRelease(ctx)
                                }
                            }
                        }

                        Bitmap32(width, height, RgbaArray(out), premultiplied = premultiplied).apply {
                            //flipY()
                        }
                    } finally {
                        CGColorSpaceRelease(colorSpace)
                    }
                }
            }
        }).await(), premultiplied)
    }
}
