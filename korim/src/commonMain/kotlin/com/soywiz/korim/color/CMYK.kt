package com.soywiz.korim.color

object CMYK {
    fun cmykToRgb(c: Int, m: Int, y: Int, k: Int): RGBA = RGBA(
        255 - clampTo8bit(c * (1 - k / 255) + k),
        255 - clampTo8bit(m * (1 - k / 255) + k),
        255 - clampTo8bit(y * (1 - k / 255) + k),
        0xFF
    )

    private fun clampTo8bit(a: Int): Int = if (a < 0) 0 else if (a > 255) 255 else a
}
