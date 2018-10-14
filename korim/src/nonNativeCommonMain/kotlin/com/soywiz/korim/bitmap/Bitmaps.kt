package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*

actual object Bitmaps {
    actual val transparent: BitmapSlice<Bitmap32> = Bitmap32(1, 1).slice(name = "transparent")
    actual val white: BitmapSlice<Bitmap32> = Bitmap32(1, 1, RgbaArray(1) { Colors.WHITE }).slice(name = "white")
}

