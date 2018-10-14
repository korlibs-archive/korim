package com.soywiz.korim.font

import com.soywiz.korim.bitmap.*
import kotlin.test.*

class NativeFontTest {
	@Test
	fun name() {
		val bmpFont = BitmapFontGenerator.generate("Arial", 64, "ABCDEFGHIJKLMNOPQRSTUVWXYZ")

		val bmp = Bitmap32(200, 200)
		//bmp.drawText(bmpFont, "HELLO")

		//awtShowImage(bmp); Thread.sleep(10000)
	}
}