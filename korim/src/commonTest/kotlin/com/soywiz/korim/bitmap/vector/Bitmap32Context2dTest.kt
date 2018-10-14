package com.soywiz.korim.bitmap.vector

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.async.*
import com.soywiz.korma.*
import kotlin.test.*

class Bitmap32Context2dTest {
	@Test
	@Ignore
	fun testVisualRendered() {
		val bitmaps = listOf(Bitmap32(128, 128), NativeImage(128, 128))
		for (bmp in bitmaps) {
			bmp.getContext2d().apply {
				//fill(Context2d.Color(Colors.BLUE))
				keep {
					scale(2.0, 1.0)
					rotateDeg(15.0)
					fill(
						Context2d.Gradient(
							Context2d.Gradient.Kind.LINEAR,
							8.0, 8.0, 0.0,
							32.0, 32.0, 1.0,
							//32.0, 8.0, 1.0,
							stops = DoubleArrayList(0.0, 1.0),
							colors = IntArrayList(Colors.BLUE.rgba, Colors.RED.rgba),
							transform = Matrix2d().scale(2.0, 0.75)
						)
					)
					if (true) {
						keep {
							beginPath()
							moveTo(8, 8)
							quadraticCurveTo(40, 0, 64, 32)
							lineTo(8, 64)
							closePath()

							//fillRect(8, 8, 32, 64)
							rect(8, 8, 32, 64)
							rectHole(16, 16, 16, 32)

							fill()
						}
					} else {
					}
				}
			}
		}
		val out = Bitmap32(256, 128)
		out.put(bitmaps[0].toBMP32(), 0, 0)
		out.put(bitmaps[1].toBMP32(), 128, 0)
		//runBlocking {
		//	showImageAndWait(out)
		//}
	}
}