package com.soywiz.korim.font

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*

object BitmapFontGenerator {
	val SPACE = " "
	val UPPERCASE = ('A'..'Z').joinToString("")
	val LOWERCASE = ('a'..'z').joinToString("")
	val NUMBERS = ('0'..'9').joinToString("")
	val PUNCTUATION = "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}"
	val LATIN_BASIC = "ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥PÉáíóúñÑª°¿¬½¼¡«»ßµø±÷°·.²"
	val LATIN_ALL = SPACE + UPPERCASE + LOWERCASE + NUMBERS + PUNCTUATION + LATIN_BASIC

	fun generate(fontName: String, fontSize: Number, chars: String, mipmaps: Boolean = true, fontRegistry: FontRegistry = SystemFontRegistry): BitmapFont =
		generate(fontRegistry.get(fontName, fontSize.toDouble()), chars.indices.map { chars[it].toInt() }.toIntArray(), mipmaps)

    fun generate(fontName: String, fontSize: Number, chars: IntArray, mipmaps: Boolean = true, fontRegistry: FontRegistry = SystemFontRegistry): BitmapFont =
        generate(fontRegistry.get(fontName, fontSize.toDouble()), chars.indices.map { chars[it].toInt() }.toIntArray(), mipmaps)

	fun generate(font: Font, chars: IntArray, mipmaps: Boolean = true, name: String = font.name): BitmapFont {
		val result = measureTimeWithResult {
			val bni = NativeImage(1, 1)
			val bnictx = bni.getContext2d()
			bnictx.font = font
			val bitmapHeight = bnictx.getTextBounds("a").bounds.height.toInt()

			val widths: List<Int> = chars.map { bnictx.getTextBounds("${it.toChar()}").bounds.width.toInt() }
			val widthsSum = widths.map { it + 2 }.sum()
			val ni = NativeImage(widthsSum, bitmapHeight)

			class GlyphInfo(val char: Int, val rect: RectangleInt, val width: Int)

			val g = ni.getContext2d()
			g.fillStyle = g.createColor(Colors.WHITE)
			g.font = font
			g.horizontalAlign = HorizontalAlign.LEFT
			g.verticalAlign = VerticalAlign.TOP
			val glyphsInfo = arrayListOf<GlyphInfo>()
			var x = 0
			val itemp = IntArray(1)
			for ((index, char) in chars.withIndex()) {
				val width = widths[index]
				itemp[0] = char
				g.fillText(String_fromIntArray(itemp, 0, 1), x.toDouble(), 0.0)
				glyphsInfo += GlyphInfo(char, RectangleInt(x, 0, width, ni.height), width)
				x += width + 2
			}

			val atlas = ni.toBMP32()

			BitmapFont(
				atlas, font.size.toInt(), font.size.toInt(), font.size.toInt(),
				glyphsInfo.associate {
					it.char to BitmapFont.Glyph(it.char, atlas.slice(it.rect), 0, 0, it.width)
				}.toIntMap(),
				IntMap(),
                name = name
			)
		}

		return result.result
	}
}

operator fun BitmapFont.Companion.invoke(
	fontName: String,
	fontSize: Int,
	chars: String = BitmapFontGenerator.LATIN_ALL,
	mipmaps: Boolean = true
): BitmapFont = BitmapFontGenerator.generate(fontName, fontSize, chars, mipmaps)

operator fun BitmapFont.Companion.invoke(
    font: Font,
    chars: String = BitmapFontGenerator.LATIN_ALL,
    mipmaps: Boolean = true
): BitmapFont = BitmapFontGenerator.generate(font, chars.map { it.toInt() }.toIntArray(), mipmaps)
