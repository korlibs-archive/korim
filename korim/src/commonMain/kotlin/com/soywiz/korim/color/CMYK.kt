package com.soywiz.korim.color

import com.soywiz.kmem.*
import com.soywiz.korim.internal.*

data class CMYK(val value: Int) {
    val c: Int get() = value.extract8(0)
    val m: Int get() = value.extract8(8)
    val y: Int get() = value.extract8(16)
    val k: Int get() = value.extract8(24)

    companion object {
    }
}

fun CMYK.toRGBA() = RGBA(
    255 - (c * (1 - k / 255) + k).clamp0_255(),
    255 - (m * (1 - k / 255) + k).clamp0_255(),
    255 - (y * (1 - k / 255) + k).clamp0_255(),
    0xFF
)
