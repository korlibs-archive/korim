package com.soywiz.korim.bitmap

import com.soywiz.korma.geom.IRectangle

fun <T : Bitmap> T.sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int): BitmapSlice<T> = BitmapSlice<T>(this, IRectangle(left, top, right - left, bottom - top))
fun <T : Bitmap> T.sliceWithSize(x: Int, y: Int, width: Int, height: Int): BitmapSlice<T> = BitmapSlice<T>(this, IRectangle(x, y, width, height))
fun <T : Bitmap> T.slice(bounds: IRectangle): BitmapSlice<T> = BitmapSlice<T>(this, bounds)
