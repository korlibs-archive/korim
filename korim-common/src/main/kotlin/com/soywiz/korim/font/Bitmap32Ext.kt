package com.soywiz.korim.font

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors

fun Bitmap32.drawText(font: BitmapFont, str: String, x: Int = 0, y: Int = 0, color: Int = Colors.WHITE) = font.drawText(this, str, x, y, color)
