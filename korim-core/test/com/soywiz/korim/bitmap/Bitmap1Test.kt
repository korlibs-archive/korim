package com.soywiz.korim.bitmap

import com.soywiz.korio.util.splitInChunks
import com.soywiz.korio.util.toHexString
import org.junit.Assert
import org.junit.Test

class Bitmap1Test {
	@Test
	fun name() {
		val bmp = Bitmap1(4, 4)
		Assert.assertEquals(
			"""
				....
				....
				....
				....
			""".trimIndent(),
			bmp.toLines(".X").joinToString("\n")
		)

		bmp[0, 0] = 1
		bmp[1, 1] = 1
		bmp[1, 2] = 1
		bmp[3, 3] = 1

		Assert.assertEquals(
			"""
				X...
				.X..
				.X..
				...X
			""".trimIndent(),
			bmp.toLines(".X").joinToString("\n")
		)

		Assert.assertEquals(
			"21:82",
			bmp.data.toHexString().splitInChunks(2).joinToString(":")
		)
	}
}