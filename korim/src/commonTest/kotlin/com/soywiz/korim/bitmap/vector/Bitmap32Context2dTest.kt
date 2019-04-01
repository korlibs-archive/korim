package com.soywiz.korim.bitmap.vector

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korio.util.encoding.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class Bitmap32Context2dTest {
    @Test
    fun testVisualRendered() = suspendTest {
        if (OS.isMac) return@suspendTest // Ignore on MAC since this fails on travis on K/N?

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
                            colors = IntArrayList(Colors.BLUE.value, Colors.RED.value),
                            transform = Matrix().scale(2.0, 0.75)
                        )
                    )
                    if (true) {
                        keep {
                            beginPath()
                            moveTo(8, 8)
                            quadTo(40, 0, 64, 32)
                            lineTo(8, 64)
                            close()

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
        //showImageAndWait(out)
        //}
    }

    @Test
    fun renderContext2dWithImage() = suspendTest {
        val pngBytes = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgAQMAAABJtOi3AAAAA1BMVEVrVPMZmyLtAAAAC0lEQVR4AWMY5AAAAKAAAVQqnscAAAAASUVORK5CYII=".fromBase64()
        PNG.decode(pngBytes)

        val img = nativeImageFormatProvider.decode(pngBytes)

        val rendered = NativeImage(128, 128).context2d {
            rect(0, 0, 100, 100)
            fill(Context2d.BitmapPaint(img, Matrix()))
        }
        val bmp = rendered.toBMP32()

        // @TODO: This should work on native too!
        if (!OS.isNative) {
            assertEquals("#6b54f3ff", bmp[0, 0].hexString)
            assertEquals("#6b54f3ff", bmp[31, 31].hexString)
            //assertEquals("#6b54f3ff", bmp[99, 99].hexString) // @TODO: This should work too on Node.JS or should not work on JVM?
            assertEquals("#00000000", bmp[101, 101].hexString)
        }
    }
}
