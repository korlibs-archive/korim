package com.soywiz.korim.html

import com.jtransc.js.*
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.NativeFont
import com.soywiz.korim.font.NativeFontProvider
import com.soywiz.korim.geom.IRect

class HtmlFont(fontName: String, size: Double) : NativeFont(fontName, size) {
	val metricsCanvas = document.methods["createElement"]("canvas")
	val metricsCtx = metricsCanvas.methods["getContext"]("2d")

	override fun getGlyphs(chars: IntArray): BitmapFont {
		metricsCtx["font"] = "${size}px $fontName"
		metricsCtx["textAlign"] = "left"
		metricsCtx["textBaseline"] = "top"
		metricsCtx["fillStyle"] = "white"

		val widths = chars.map { metricsCtx.methods["measureText"](String(charArrayOf(it.toChar())))["width"].toInt() }
		val widthsSum = widths.map { it + 2 }.sum()

		val height = (size * 1.2).toInt()
		val canvas = document.methods["createElement"]("canvas")
		canvas["style"]["width"] = "${widthsSum}px"
		canvas["style"]["height"] = "${height}px"
		canvas["width"] = widthsSum
		canvas["height"] = height
		val ctx = canvas.methods["getContext"]("2d")
		ctx["font"] = "${size}px $fontName"
		ctx["textAlign"] = "left"
		ctx["textBaseline"] = "top"
		ctx["fillStyle"] = "white"
		val glyphs = arrayListOf<BitmapFont.GlyphInfo>()
		var x = 0
		for ((index, char) in chars.withIndex()) {
			val width = widths[index]
			ctx.methods["fillText"](String(intArrayOf(char), 0, 1), x, 0)
			glyphs += BitmapFont.GlyphInfo(char, IRect(x, 0, width, height), width)
			x += width + 2
		}
		return BitmapFont(CanvasNativeImage(canvas).toBMP32(), size.toInt(), size.toInt(), glyphs)
	}
}

class HtmlFontProvider : NativeFontProvider {
	override fun getNativeFont(fontName: String, fontSize: Double): NativeFont = HtmlFont(fontName, fontSize)
}