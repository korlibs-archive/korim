package com.soywiz.korim.font.ttf

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korio.vfs.applicationVfs
import org.junit.Before
import org.junit.Test
import kotlin.test.Ignore

class TtfFontTest {
	lateinit var root: VfsFile

	@Before
	fun before() = syncTest {
		for (path in listOf(applicationVfs["src/test/resources"], ResourcesVfs)) {
			root = path
			if (root["kotlin8.png"].exists()) break
		}
	}

	@Test
	@Ignore
	fun name() = syncTest {
		val font = TtfFont(root["Comfortaa-Regular.ttf"].readAll().openSync())
		showImageAndWait(NativeImage(512, 128).apply {
			getContext2d()
				.fillText(font, "HELLO WORLD. This 0123 ñáéíóúç", size = 32.0, x = 0.0, y = 0.0, color = Colors.RED, origin = TtfFont.Origin.TOP)
		})
	}
}
