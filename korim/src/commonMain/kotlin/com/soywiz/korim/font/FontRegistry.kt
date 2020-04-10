package com.soywiz.korim.font

import com.soywiz.kds.CopyOnWriteFrozenMap
import kotlin.native.concurrent.ThreadLocal

interface FontRegistry {
    operator fun get(name: String): Font = SystemFont(name)
    companion object {
        operator fun invoke(): DefaultFontRegistry = DefaultFontRegistry()
    }
}

@ThreadLocal
object SystemFontRegistry : DefaultFontRegistry() {
    val DEFAULT_FONT = this.get("sans-serif")
}

open class DefaultFontRegistry : FontRegistry {
    private val registeredFonts = CopyOnWriteFrozenMap<String, Font>()
    fun register(font: Font, name: String = font.name) = font.also { registeredFonts[name] = it }
    override operator fun get(name: String): Font = registeredFonts[name] ?: SystemFont(name)
}
