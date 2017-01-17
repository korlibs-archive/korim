package com.soywiz.korim.font

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.geom.IRectangle
import com.soywiz.korim.vector.Context2d
import java.awt.Font
import java.awt.image.BufferedImage

object BitmapFontGenerator {
	fun generate(fontName: String, fontSize: Int, chars: String): BitmapFont = generate(fontName, fontSize, chars.indices.map { chars[it].toInt() }.toIntArray())

	fun generate(fontName: String, fontSize: Int, chars: IntArray): BitmapFont {
		val font = Font(fontName, Font.PLAIN, fontSize.toInt())
		val metrics = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics().getFontMetrics(font)

		val widths = chars.map { metrics.charWidth(it) }
		val widthsSum = widths.map { it + 2 }.sum()
		val ni = NativeImage(widthsSum, (fontSize * 1.2).toInt())
		val g = ni.getContext2d()
		g.fillStyle = g.createColor(Colors.WHITE)
		g.font = Context2d.Font(fontName, fontSize.toDouble())
		g.verticalAlign = Context2d.VerticalAlign.TOP
		val glyphs = arrayListOf<BitmapFont.GlyphInfo>()
		var x = 0
		for ((index, char) in chars.withIndex()) {
			val width = widths[index]
			g.fillText(String(intArrayOf(char), 0, 1), x.toDouble(), 6.0)
			glyphs += BitmapFont.GlyphInfo(char, IRectangle(x, 0, width, ni.height), width)
			x += width + 2
		}
		return BitmapFont(ni.toBMP32(), fontSize.toInt(), fontSize.toInt(), glyphs)

	}
}