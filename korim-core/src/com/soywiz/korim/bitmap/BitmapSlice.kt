package com.soywiz.korim.bitmap

import com.soywiz.korim.geom.IRectangle

class BitmapSlice<out T : Bitmap>(val bmp: T, val bounds: IRectangle) {
}