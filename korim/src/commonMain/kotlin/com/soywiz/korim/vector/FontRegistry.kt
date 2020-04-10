package com.soywiz.korim.vector

import com.soywiz.kds.CopyOnWriteFrozenMap
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.ttf.TtfFont

interface FontRegistry {
    fun get(name: String, size: Double): Font = Font.System(this, name, size)
    companion object {
        operator fun invoke(): DefaultFontRegistry = DefaultFontRegistry()
    }
}

object SystemFontRegistry : DefaultFontRegistry() {
    val DEFAULT_FONT = this.get("sans-serif", 10.0)
}

open class DefaultFontRegistry : FontRegistry {
    private val registeredFonts = CopyOnWriteFrozenMap<String, Font>()
    fun register(font: TtfFont, name: String = font.name) = run { registeredFonts[name] = Font.Ttf(this, font, name, 0.0) }
    fun register(font: BitmapFont, name: String = font.name) = run { registeredFonts[name] = Font.Bitmap(this, font, name, 0.0) }
    override fun get(name: String, size: Double): Font =
        registeredFonts[name]?.clone(size = size) ?: Font.System(this, name, size)
}
