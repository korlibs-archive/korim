package com.soywiz.korim.font

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.lang.String_fromIntArray
import com.soywiz.korma.geom.RectangleInt

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
		val bitmapHeight = bnictx.getTextBounds("a").bounds.height.toInt()

		val widths: List<Int> = chars.map { bnictx.getTextBounds("${it.toChar()}").bounds.width.toInt() }
		val widthsSum = widths.map { it + 2 }.sum()
		val ni = NativeImage(widthsSum, bitmapHeight)

		//println("BitmapFont:")
		//println("bitmapHeight=$bitmapHeight")
		//for ((index, width) in widths.withIndex()) {
		//	val char = chars[index]
		//	println("$index: $char: width=$width")
		//}

		val g = ni.getContext2d()
		g.fillStyle = g.createColor(Colors.WHITE)
		g.font = Context2d.Font(fontName, fontSize.toDouble())
		g.horizontalAlign = Context2d.HorizontalAlign.LEFT
		g.verticalAlign = Context2d.VerticalAlign.TOP
		val glyphs = arrayListOf<BitmapFont.GlyphInfo>()
		var x = 0
		val itemp = IntArray(1)
		for ((index, char) in chars.withIndex()) {
			val width = widths[index]
			itemp[0] = char
			g.fillText(String_fromIntArray(itemp, 0, 1), x.toDouble(), 0.0)
			glyphs += BitmapFont.GlyphInfo(char, RectangleInt(x, 0, width, ni.height), width)
			x += width + 2
		}

		println("BitmapFontGenerator.generate($fontName, $fontSize, $chars, premult=${ni.premult})")

		return BitmapFont(ni.toBMP32(), fontSize, fontSize, glyphs)

	}
}