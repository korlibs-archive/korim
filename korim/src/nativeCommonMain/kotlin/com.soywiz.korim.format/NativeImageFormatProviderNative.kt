package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlin.math.*

open class BaseNativeNativeImageFormatProvider : NativeImageFormatProvider() {
    override suspend fun decode(data: ByteArray, premultiplied: Boolean): NativeImage = wrapNative(RegisteredImageFormats.decode(data), premultiplied)
    override suspend fun decode(vfs: Vfs, path: String, premultiplied: Boolean): NativeImage = decode(vfs[path].readBytes(), premultiplied)
    protected fun wrapNative(bmp: Bitmap, premultiplied: Boolean): BitmapNativeImage {
        val bmp32: Bitmap32 = bmp.toBMP32()
        //bmp32.premultiplyInPlace()
        //return BitmapNativeImage(bmp32)
        return BitmapNativeImage(if (premultiplied) bmp32.premultipliedIfRequired() else bmp32.depremultipliedIfRequired())
    }
    override fun create(width: Int, height: Int): NativeImage = BitmapNativeImage(Bitmap32(width, height))
    override fun copy(bmp: Bitmap): NativeImage = BitmapNativeImage(bmp)
    override suspend fun display(bitmap: Bitmap, kind: Int) {
        println("TODO: NativeNativeImageFormatProvider.display(bitmap=$bitmap, kind=$kind)")
    }
    override fun mipmap(bmp: Bitmap, levels: Int): NativeImage = BitmapNativeImage(bmp)
    override fun mipmap(bmp: Bitmap): NativeImage = BitmapNativeImage(bmp)
}
