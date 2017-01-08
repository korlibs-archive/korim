package com.soywiz.korim.color

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.util.splitInChunks
import com.soywiz.korio.util.toHexString
import org.junit.Assert
import org.junit.Test

class ColorFormatTest {
	fun ByteArray.toHexChunks(size: Int) = this.toHexString().splitInChunks(size).joinToString("-")

	@Test
	fun name() {
		val bmp = Bitmap32(3, 1, intArrayOf(Colors.RED, Colors.GREEN, Colors.BLUE))
		Assert.assertEquals("0000FF-00FF00-FF0000", RGB.encode(bmp.data, littleEndian = false).toHexChunks(6))
		Assert.assertEquals("FF0000FF-FF00FF00-FFFF0000", RGBA.encode(bmp.data, littleEndian = false).toHexChunks(8))
		Assert.assertEquals("FFFF004C-FF000095-FF00FF1D", YUVA.encode(bmp.data, littleEndian = false).toHexChunks(8))

		Assert.assertEquals("F00F-F0F0-FF00", RGBA_4444.encode(bmp.data, littleEndian = false).toHexChunks(4))
		Assert.assertEquals("001F-07E0-F800", RGBA_5650.encode(bmp.data, littleEndian = false).toHexChunks(4))
		Assert.assertEquals("801F-83E0-FC00", RGBA_5551.encode(bmp.data, littleEndian = false).toHexChunks(4))

		Assert.assertEquals("FF00-F0F0-F00F", BGRA_4444.encode(bmp.data, littleEndian = false).toHexChunks(4))
		Assert.assertEquals("F800-07E0-001F", BGRA_5650.encode(bmp.data, littleEndian = false).toHexChunks(4))
		Assert.assertEquals("FC00-83E0-801F", BGRA_5551.encode(bmp.data, littleEndian = false).toHexChunks(4))
	}
}