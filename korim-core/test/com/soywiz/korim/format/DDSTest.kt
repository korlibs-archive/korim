package com.soywiz.korim.format

import com.soywiz.korim.bitmap.matchContents
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Assert
import org.junit.Test

class DDSTest {
	@Test
	fun dxt1() = syncTest {
		val output = ResourcesVfs["dxt1.dds"].readBitmapNoNative()
		val expected = ResourcesVfs["dxt1.png"].readBitmapNoNative()
		Assert.assertTrue(output.matchContents(expected))
	}

	@Test
	fun dxt3() = syncTest {
		val output = ResourcesVfs["dxt3.dds"].readBitmapNoNative()
		val expected = ResourcesVfs["dxt3.png"].readBitmapNoNative()
		Assert.assertTrue(output.matchContents(expected))
	}

	@Test
	fun dxt5() = syncTest {
		val output = ResourcesVfs["dxt5.dds"].readBitmapNoNative()
		val expected = ResourcesVfs["dxt5.png"].readBitmapNoNative()
		Assert.assertTrue(output.matchContents(expected))
	}
}