package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.*
import kotlin.test.*

class ShapeTest {
	@Test
	@Ignore
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
			paint = Context2d.BitmapPaint(Bitmap32(100, 100, Colors.RED, premult = false), Matrix2d()),
			transform = Matrix2d()
		)
		assertEquals(
			"""<svg width="100px" height="100px" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs><pattern id="def0" patternUnits="userSpaceOnUse" width="100" height="100" patternTransform="scale(1, 1)"><image xlink:href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAYAAABw4pVUAAAA/UlEQVR42u3RoQ0AMBDEsNt/6e8YDTAwj5TddnTsdwCGpBkSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgS8wBKb9ZkYYEq8QAAAABJRU5ErkJggg==" width="100" height="100"/></pattern></defs><g transform="scale(1, 1)"><path d="M0 0L100 100L0 100Z" fill="url(#def0)"/></g></svg>""",
			shape.toSvg().outerXml
		)
	}
}
