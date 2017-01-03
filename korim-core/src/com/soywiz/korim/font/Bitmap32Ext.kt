package com.soywiz.korim.font

import com.soywiz.korim.bitmap.Bitmap32

fun Bitmap32.drawText(font: BitmapFont, str: String, x: Int = 0, y: Int = 0) = font.drawText(this, str, x, y)
