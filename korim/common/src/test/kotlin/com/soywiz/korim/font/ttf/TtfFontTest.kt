package com.soywiz.korim.font.ttf

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.openSync
import org.junit.Test
import kotlin.test.Ignore

class TtfFontTest {
	@Test
	@Ignore
	fun name() = syncTest {
		val font = TtfFontReader(tinymce_small_ttf.openSync())
		val glyphs = font.readAllGlyphs()
		//font.readGlyphsSuspend()
	}
}
