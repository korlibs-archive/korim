package com.soywiz.korim.vector

import org.junit.Assert
import org.junit.Test

class GraphicsPathTest {
	@Test
	fun testSimpleSquare() {
		val g = GraphicsPath()
		g.moveTo(0, 0)
		g.lineTo(100, 0)
		g.lineTo(100, 100)
		g.lineTo(0, 100)
		g.close()

		Assert.assertEquals(true, g.containsPoint(50, 50))
		Assert.assertEquals(false, g.containsPoint(150, 50))
	}

	@Test
	fun testSquareWithHole() {
		val g = GraphicsPath()
		g.moveTo(0, 0)
		g.lineTo(100, 0)
		g.lineTo(100, 100)
		g.lineTo(0, 100)
		g.close()

		g.moveTo(75, 25)
		g.lineTo(25, 25)
		g.lineTo(25, 75)
		g.lineTo(75, 75)
		g.close()

		Assert.assertEquals(false, g.containsPoint(50, 50))
		Assert.assertEquals(false, g.containsPoint(150, 50))
		Assert.assertEquals(true, g.containsPoint(20, 50))
		//g.filled(Context2d.Color(Colors.RED)).raster().showImageAndWaitExt()
	}
}