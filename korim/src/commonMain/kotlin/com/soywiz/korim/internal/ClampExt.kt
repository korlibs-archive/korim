package com.soywiz.korim.internal

internal fun Int.clamp0_255(): Int {
    val n = this and -(if (this >= 0) 1 else 0)
    return (n or (255 - n shr 31)) and 0xFF
}
