package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import kotlin.test.*

class PSDTest : BaseImageFormatTest() {
	val formats = ImageFormats(StandardImageFormats + PSD)
	val ResourcesVfs = root

	@Test
	fun psdTest() = suspendTest {
		val output = ResourcesVfs["small.psd"].readBitmapNoNative(formats)
		val expected = ResourcesVfs["small.psd.png"].readBitmapNoNative(formats)
		//showImageAndWait(output)
		assertEquals(0, output.matchContentsDistinctCount(expected))
	}
}