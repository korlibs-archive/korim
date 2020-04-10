package com.soywiz.korim.font

import com.soywiz.kds.CopyOnWriteFrozenMap
import kotlin.native.concurrent.ThreadLocal

interface FontRegistry {
    fun get(name: String, size: Double): Font =
        SystemFont(name, size, this)
    companion object {
        operator fun invoke(): DefaultFontRegistry =
            DefaultFontRegistry()
    }
}
inline fun FontRegistry.get(name: String, size: Number) = this.get(name, size.toDouble())

@ThreadLocal
object SystemFontRegistry : DefaultFontRegistry() {
    val DEFAULT_FONT = this.get("sans-serif", 10.0)
}

open class DefaultFontRegistry : FontRegistry {
    private val registeredFonts = CopyOnWriteFrozenMap<String, Font>()
    fun register(font: Font, name: String = font.name) = font.also { registeredFonts[name] = it }
    override fun get(name: String, size: Double): Font =
        registeredFonts[name]?.clone(size = size) ?: SystemFont(
            name,
            size,
            this
        )
}
