package com.soywiz.korim.font.ttf

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.compression.uncompressGzip
import com.soywiz.korio.stream.openSync
import org.junit.Test
import kotlin.test.Ignore

class TtfFontTest {
	@Test
	@Ignore
	fun name() = syncTest {
		val font = TtfFont(optimusPrincepsTtf.uncompressGzip().openSync())
		showImageAndWait(NativeImage(512, 128).apply {
			getContext2d()
				.fillText(font, "HELLO WORLD. This 0123", size = 32.0, x = 0.0, y = 0.0, color = Colors.RED, origin = TtfFont.Origin.TOP)
		})
	}
}
