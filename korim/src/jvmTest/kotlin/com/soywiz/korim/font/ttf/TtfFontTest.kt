package com.soywiz.korim.font.ttf

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.stream.*
import kotlin.test.*

class TtfFontTest {
	lateinit var root: VfsFile

	fun ttfTest(callback: suspend () -> Unit) = suspendTest {
		for (path in listOf(applicationVfs["src/test/resources"], ResourcesVfs)) {
			root = path
			if (root["kotlin8.png"].exists()) break
		}
		callback()
	}

	@Test
	@Ignore
	fun name() = ttfTest {
		val font = TtfFont(root["Comfortaa-Regular.ttf"].readAll().openSync())
		showImageAndWait(NativeImage(512, 128).apply {
			getContext2d()
				.fillText(
					font,
					"HELLO WORLD. This 0123 ñáéíóúç",
					size = 32.0,
					x = 0.0,
					y = 0.0,
					color = Colors.RED,
					origin = TtfFont.Origin.TOP
				)
		})
	}
}
