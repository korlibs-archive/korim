package com.soywiz.korim.font

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.RectangleInt

class BitmapFont(
	val atlas: Bitmap32,
	val size: Int,
	val lineHeight: Int,
	val glyphInfos: List<GlyphInfo>
) {
	val glyphsById = glyphInfos.map { it.id to Glyph(atlas.slice(it.bounds), it) }.toMap()

	fun measureWidth(text: String): Int {
		var x = 0
		for (c in text) {
			val glyph = glyphsById[c.toInt()]
			if (glyph != null) x += glyph.advance
		}
		return x
	}

	fun drawText(bmp: Bitmap32, str: String, x: Int = 0, y: Int, color: Int = Colors.WHITE) {
		var py = y
		var px = x
		for (c in str) {
			val g = glyphsById[c.toInt()]
			if (g != null) {
				bmp.draw(g.bmp, px, py)
				px += g.advance
			}
			if (c == '\n') {
				py += lineHeight
				px = x
			}
		}
	}

	data class Glyph(val bmp: BitmapSlice<Bitmap32>, val info: GlyphInfo) {
		val advance = info.advance
	}

	data class GlyphInfo(val id: Int, val bounds: RectangleInt, val advance: Int)
}