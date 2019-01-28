package com.soywiz.korim.internal

fun packIntUnchecked(r: Int, g: Int, b: Int, a: Int): Int {
    return (((r and 0xFF) shl 0) or ((g and 0xFF) shl 8) or ((b and 0xFF) shl 16) or ((a and 0xFF) shl 24))
}

fun packIntClamped(r: Int, g: Int, b: Int, a: Int): Int {
    return packIntUnchecked(r.clamp0_255(), g.clamp0_255(), b.clamp0_255(), a.clamp0_255())
}
