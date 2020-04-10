package com.soywiz.korim.vector

import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.ttf.TtfFont

interface Font {
    val name: String
    val size: Double

    data class System(override val name: String, override val size: Double) : Font
    data class Bitmap(val bitmap: BitmapFont, override val size: Double) : Font {
        override val name get() = bitmap.name
    }
    data class Ttf(val ttf: TtfFont, override val size: Double) : Font {
        override val name get() = ttf.name
    }

    companion object {
        operator fun invoke(name: String, size: Double) = System(name, size)
        operator fun invoke(bitmap: BitmapFont, size: Double) = Bitmap(bitmap, size)
        operator fun invoke(ttf: TtfFont, size: Double) = Ttf(ttf, size)
    }
}
