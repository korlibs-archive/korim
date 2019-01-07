package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.file.*
import kotlinx.cinterop.*
import platform.gdiplus.*
import platform.windows.*

actual val nativeImageFormatProvider: NativeImageFormatProvider = object : BaseNativeNativeImageFormatProvider() {
    override suspend fun decode(data: ByteArray): NativeImage = wrapNative(decodeImageSync(data))
}

private var initializedGdiPlus = false
private fun initGdiPlusOnce() {
    if (initializedGdiPlus) return
    initializedGdiPlus = true
    memScoped {
        val ptoken = allocArray<ULONG_PTRVar>(1)
        val si = alloc<GdiplusStartupInput>().apply {
            GdiplusVersion = 1
            DebugEventCallback = null
            SuppressExternalCodecs = FALSE
            SuppressBackgroundThread = FALSE
        }
        GdiplusStartup(ptoken, si.ptr, null)
    }
}

private fun decodeImageSync(data: ByteArray): Bitmap32 = memScoped {
    val width = alloc<FloatVar>()
    val height = alloc<FloatVar>()
    val pimage = allocArray<COpaquePointerVar>(1)

    initGdiPlusOnce()
    data.usePinned { datap ->
        val pdata = datap.addressOf(0)
        val pstream = SHCreateMemStream(pdata, data.size)!!
        try {
            if (GdipCreateBitmapFromStream(pstream, pimage) != 0) {
                throw RuntimeException("Can't load image from byte array")
            }
        } finally {
            pstream.pointed.lpVtbl?.pointed?.Release?.invoke(pstream)
        }
    }

    GdipGetImageDimension(pimage[0], width.ptr, height.ptr)

    val rect = alloc<GpRect>().apply {
        X = 0
        Y = 0
        Width = width.value.toInt()
        Height = height.value.toInt()
    }
    val bmpData = alloc<BitmapData>()
    if (GdipBitmapLockBits(pimage[0], rect.ptr, ImageLockModeRead, PixelFormat32bppARGB, bmpData.ptr) != 0) {
        throw RuntimeException("Can't lock image")
    }

    val out = IntArray(bmpData.Width * bmpData.Height)
    out.usePinned { outp ->
        val o = outp.addressOf(0)
        for (y in 0 until bmpData.Height) {
            memcpy(o, (bmpData.Scan0.toLong() + (bmpData.Stride * y)).toCPointer<IntVar>(), bmpData.Width * 4)
        }
    }

    GdipBitmapUnlockBits(pimage[0], bmpData.ptr)
    GdipDisposeImage(pimage[0])

    //println(out.toList())
    Bitmap32(width.value.toInt(), height.value.toInt(), out, premult = false)
}
