package com.soywiz.korim.font

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.vector.Context2d
import com.soywiz.korma.geom.IRectangle

object BitmapFontGenerator {
	fun generate(fontName: String, fontSize: Int, chars: String): BitmapFont = generate(fontName, fontSize, chars.indices.map { chars[it].toInt() }.toIntArray())

	val SPACE = " "
	val UPPERCASE = ('A'..'Z').joinToString("")
	val LOWERCASE = ('a'..'z').joinToString("")
	val NUMBERS = ('0'..'9').joinToString("")
	val PUNCTUATION = "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}"
	val LATIN_BASIC = "ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥PÉáíóúñÑª°¿¬½¼¡«»ßµø±÷°·.²"
	val LATIN_ALL = SPACE + UPPERCASE + LOWERCASE + NUMBERS + PUNCTUATION + LATIN_BASIC

	val bni = NativeImage(1, 1)

	fun generate(fontName: String, fontSize: Int, chars: IntArray): BitmapFont {
		val bnictx = bni.getContext2d()
		bnictx.font = Context2d.Font(fontName, fontSize.toDouble())
		val bitmapHeight = bnictx.getTextBounds("a").bounds.height

		val widths = chars.map { bnictx.getTextBounds("" + it.toChar()).bounds.width.toInt() }
		val widthsSum = widths.map { it + 2 }.sum()
		val ni = NativeImage(widthsSum.toInt(), bitmapHeight.toInt())

		val g = ni.getContext2d()
		g.fillStyle = g.createColor(Colors.WHITE)
		g.font = Context2d.Font(fontName, fontSize.toDouble())
		g.horizontalAlign = Context2d.HorizontalAlign.LEFT
		g.verticalAlign = Context2d.VerticalAlign.TOP
		val glyphs = arrayListOf<BitmapFont.GlyphInfo>()
		var x = 0
		for ((index, char) in chars.withIndex()) {
			val width = widths[index]
			g.fillText(String(intArrayOf(char), 0, 1), x.toDouble(), 0.0)
			glyphs += BitmapFont.GlyphInfo(char, IRectangle(x, 0, width, ni.height), width)
			x += width + 2
		}
		return BitmapFont(ni.toBMP32(), fontSize.toInt(), fontSize.toInt(), glyphs)

	}
}