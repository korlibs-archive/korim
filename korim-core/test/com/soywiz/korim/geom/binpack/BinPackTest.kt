package com.soywiz.korim.geom.binpack

import com.soywiz.korim.geom.Size
import com.soywiz.korim.geom.binpack.BinPacker
import com.soywiz.korio.async.syncTest
import org.junit.Assert
import org.junit.Test

class BinPackTest {
	@Test
	fun name() = syncTest {
		val factory = BinPacker(100.0, 100.0)
		val result = factory.addBatch(listOf(Size(20, 10), Size(10, 30), Size(100, 20), Size(20, 80)))
		//result.filterNotNull().render().showImageAndWaitExt()
		Assert.assertEquals(
				"[Rectangle(20.0, 50.0, 20.0, 10.0), Rectangle(20.0, 20.0, 10.0, 30.0), Rectangle(0.0, 0.0, 100.0, 20.0), Rectangle(0.0, 20.0, 20.0, 80.0)]",
				result.toString()
		)
	}
}