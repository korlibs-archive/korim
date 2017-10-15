package com.soywiz.korim.format

import com.soywiz.korim.bitmap.matchContents
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Test
import kotlin.test.assertTrue

class DDSTest {
	val formats = ImageFormats().registerStandard().register(DDS)

	@Test
	fun dxt1() = syncTest {
		val output = ResourcesVfs["dxt1.dds"].readBitmapNoNative(formats)
		val expected = ResourcesVfs["dxt1.png"].readBitmapNoNative(formats)
		assertTrue(output.matchContents(expected))
	}

	@Test
	fun dxt3() = syncTest {
		val output = ResourcesVfs["dxt3.dds"].readBitmapNoNative(formats)
		val expected = ResourcesVfs["dxt3.png"].readBitmapNoNative(formats)
		assertTrue(output.matchContents(expected))
		//output.writeTo(LocalVfs("c:/temp/dxt3.png"))
	}

	@Test
	fun dxt5() = syncTest {
		val output = ResourcesVfs["dxt5.dds"].readBitmapNoNative(formats)
		val expected = ResourcesVfs["dxt5.png"].readBitmapNoNative(formats)
		assertTrue(output.matchContents(expected))
		//output.writeTo(LocalVfs("c:/temp/dxt5.png"))
	}
}