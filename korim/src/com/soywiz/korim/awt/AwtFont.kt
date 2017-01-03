package com.soywiz.korim.awt

import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.NativeFont
import com.soywiz.korim.font.NativeFontProvider
import com.soywiz.korim.geom.IRect
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage

class AwtFont(fontName: String, size: Double) : NativeFont(fontName, size) {
	val font = Font(fontName, Font.PLAIN, size.toInt())
	val metrics = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics().getFontMetrics(font)

	override fun getGlyphs(chars: IntArray): BitmapFont {
		val widths = chars.map { metrics.charWidth(it) }
		val widthsSum = widths.map { it + 2 }.sum()
		val bi = BufferedImage(widthsSum, (size * 1.2).toInt(), BufferedImage.TYPE_INT_ARGB)
		val g = bi.createGraphics()
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
		g.paint = Color.white
		g.font = font
		val glyphs = arrayListOf<BitmapFont.GlyphInfo>()
		var x = 0
		for ((index, char) in chars.withIndex()) {
			val width = widths[index]
			g.drawString(String(intArrayOf(char), 0, 1), x, metrics.ascent)
			glyphs += BitmapFont.GlyphInfo(char, IRect(x, 0, width, bi.height), width)
			x += width + 2
		}
		return BitmapFont(AwtNativeImage(bi).toBMP32(), size.toInt(), size.toInt(), glyphs)
	}
}

class AwtFontProvider : NativeFontProvider {
	override fun getNativeFont(fontName: String, fontSize: Double): NativeFont = AwtFont(fontName, fontSize)
}