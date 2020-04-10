package com.soywiz.korim.vector

import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.ttf.TtfFont

interface Font {
    val registry: FontRegistry
    val name: String
    val size: Double
    fun clone(name: String = this.name, size: Double = this.size) = registry.get(name, size)
    fun withSize(size: Double): Font

    data class System(override val registry: FontRegistry, override val name: String, override val size: Double) : Font {
        override fun withSize(size: Double): Font = copy(size = size)
    }
    data class Bitmap(override val registry: FontRegistry, val bitmap: BitmapFont, override val name: String = bitmap.name, override val size: Double) : Font {
        override fun withSize(size: Double): Font = copy(size = size)
    }
    data class Ttf(override val registry: FontRegistry, val ttf: TtfFont, override val name: String = ttf.name, override val size: Double) : Font {
        override fun withSize(size: Double): Font = copy(size = size)
    }
}
