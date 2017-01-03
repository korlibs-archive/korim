package com.soywiz.korim.font

import com.soywiz.korim.awt.awtShowImage
import com.soywiz.korim.bitmap.Bitmap32
import org.junit.Test

class NativeFontTest {
	@Test
	fun name() {
		val font = nativeFonts.getNativeFont("Arial", 64.0)
		val bmpFont = font.getGlyphs("ABCDEFGHIJKLMNOPQRSTUVWXYZ")

		val bmp = Bitmap32(200, 200)
		bmp.drawText(bmpFont, "HELLO")

		//awtShowImage(bmp); Thread.sleep(10000)

	}
}