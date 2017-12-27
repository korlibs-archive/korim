package com.soywiz.korim.format

import com.soywiz.korim.bitmap.matchContents
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Test
import kotlin.test.assertTrue

class PSDTest {
	val formats = ImageFormats().registerStandard().register(PSD)

	@Test
	fun psdTest() = syncTest {
		val output = ResourcesVfs["small.psd"].readBitmapNoNative(formats)
		val expected = ResourcesVfs["small.psd.png"].readBitmapNoNative(formats)
		//showImageAndWait(output)
		assertTrue(output.matchContents(expected))
	}
}