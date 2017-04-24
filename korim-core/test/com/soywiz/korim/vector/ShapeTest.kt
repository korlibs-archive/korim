package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors
import com.soywiz.korma.Matrix2d
import org.junit.Assert
import org.junit.Test

class ShapeTest {
	@Test
	fun name() {
		val shape = FillShape(
			path = GraphicsPath().apply {
				moveTo(0, 0)
				lineTo(100, 100)
				lineTo(0, 100)
				close()
			},
			clip = null,
			//paint = Context2d.Color(Colors.GREEN),
			paint = Context2d.BitmapPaint(Bitmap32(100, 100) { x, y -> Colors.RED }, Matrix2d()),
			transform = Matrix2d()
		)
		Assert.assertEquals(
			"""<svg height="100.0px" viewBox="0.0 0.0 100.0 100.0" width="100.0px" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs><pattern height="100" id="def0" patternTransform="matrix(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)" patternUnits="userSpaceOnUse" width="100"><image height="100" width="100" xlink:href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAYAAABw4pVUAAAA/UlEQVR42u3RoQ0AMBDEsNt/6e8YDTAwj5TddnTsdwCGpBkSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgS8wBKb9ZkYYEq8QAAAABJRU5ErkJggg=="/></pattern></defs><path d="M0.0 0.0L100.0 100.0L0.0 100.0Z" fill="url(#def0)" transform="matrix(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)"/></svg>""",
			shape.toSvg().outerXml
		)
	}
}