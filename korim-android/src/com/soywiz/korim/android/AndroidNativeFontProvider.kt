package com.soywiz.korim.android

import android.graphics.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.NativeFont
import com.soywiz.korim.font.NativeFontProvider
import com.soywiz.korim.geom.IRectangle


class AndroidNativeFontProvider : NativeFontProvider {
	override fun getNativeFont(fontName: String, fontSize: Double): NativeFont {
		return AndroidNativeFont(fontName, fontSize)
	}
}

class AndroidNativeFont(fontName: String, fontSize: Double) : NativeFont(fontName, fontSize) {
	val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		typeface = Typeface.create(fontName, Typeface.NORMAL)
		textSize = fontSize.toFloat()
		textAlign = Paint.Align.LEFT
		color = Colors.WHITE
	}

	override fun getGlyphs(ichars: IntArray): BitmapFont {
		val chars = String(ichars.map { it.toChar() }.toCharArray())
		val advances = FloatArray(chars.length)
		p.getTextWidths(chars, advances)
		val maxWidth = advances.map { it.toInt() + 2 }.sum()
		val bitmap = Bitmap.createBitmap(maxWidth, (fontSize * 1.2).toInt(), Bitmap.Config.ARGB_8888)
		val canvas = Canvas(bitmap)
		//val tempRect = Rect()
		val glyphs = arrayListOf<BitmapFont.GlyphInfo>()
		var x = 0
		for ((index, char) in chars.withIndex()) {
			val width = advances[index].toInt()
			canvas.drawText("$char", 0, 1, x.toFloat(), 0f, p)
			glyphs += BitmapFont.GlyphInfo(char.toInt(), IRectangle(x, 0, width, bitmap.height), width)
			x += width + 2
		}
		return BitmapFont(AndroidNativeImage(bitmap).toBMP32(), fontSize.toInt(), fontSize.toInt(), glyphs)
	}
}