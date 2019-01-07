package com.soywiz.korim.format

import com.soywiz.korim.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.vector.*

data class BitmapNativeImage(val bitmap: Bitmap32) : NativeImage(bitmap.width, bitmap.height, bitmap, bitmap.premult) {
    val intData: IntArray = bitmap.data.array

    constructor(bitmap: Bitmap) : this(bitmap.toBMP32())

    override fun getContext2d(antialiasing: Boolean): Context2d = bitmap.getContext2d(antialiasing)
    override fun toNonNativeBmp(): Bitmap = bitmap
}
