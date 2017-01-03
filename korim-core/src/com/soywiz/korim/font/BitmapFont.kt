package com.soywiz.korim.font

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmap32Slice
import com.soywiz.korim.geom.IRect

class BitmapFont(
	val atlas: Bitmap32,
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

	fun drawText(bmp: Bitmap32, str: String, x: Int = 0, y: Int) {
		var px = x
		for (c in str) {
			val g = glyphsById[c.toInt()]
			if (g != null) {
				bmp.draw(g.bmp, px, y)
				px += g.advance
			}
		}
	}

	data class Glyph(val bmp: Bitmap32Slice, val info: GlyphInfo) {
		val advance = info.advance
	}

	data class GlyphInfo(val id: Int, val bounds: IRect, val advance: Int)
}