package com.soywiz.korim.format

import com.soywiz.korio.async.sync
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Assert
import org.junit.Test

class ImageFormatsTest {
	@Test
	fun png() = sync {
		val bitmap = ResourcesVfs["kotlin.png"].readBitmap()
		Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
	}

	@Test
	fun png8() = sync {
		val bitmap = ResourcesVfs["kotlin8.png"].readBitmapNoNative()
		Assert.assertEquals("Bitmap8(190, 190, palette=32)", bitmap.toString())
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

	//@Test
	//fun bmp24() = sync {
	//    val bitmap = ResourcesVfs["kotlin.bmp"].readBitmap()
	//    Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
	//}
//
	//@Test
	//fun tga24() = sync {
	//    val bitmap = ResourcesVfs["kotlin.tga"].readBitmap()
	//    Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
	//}
}