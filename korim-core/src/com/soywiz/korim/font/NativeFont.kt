package com.soywiz.korim.font

import java.util.*

open class NativeFont(val fontName: String, val fontSize: Double) {
	open fun getGlyphs(chars: IntArray): BitmapFont = TODO()
	fun getGlyphs(chars: String): BitmapFont = getGlyphs(chars.map(Char::toInt).toIntArray())
}

interface NativeFontProvider {
	fun getNativeFont(fontName: String, fontSize: Double): NativeFont
}

val nativeFonts: NativeFontProvider by lazy {
	ServiceLoader.load(NativeFontProvider::class.java).firstOrNull()
		?: throw UnsupportedOperationException("NativeFontProvider not found!")
}