package com.soywiz.korim.format

import com.soywiz.korio.async.sync
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Assert
import org.junit.Test

class ImageFormatsNativeTest {
	@Test
	fun png() = sync {
		val bitmap = ResourcesVfs["kotlin.png"].readBitmap()
		Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun png8() = sync {
		val bitmap = ResourcesVfs["kotlin8.png"].readBitmap()
		Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun jpeg() = sync {
		val bitmap = ResourcesVfs["kotlin.jpg"].readBitmap()
		Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun jpeg2() = sync {
		val bitmap = ResourcesVfs["img1.jpg"].readBitmap()
		Assert.assertEquals("Bitmap32(460, 460)", bitmap.toString())
	}
}