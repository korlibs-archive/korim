package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korio.async.*
import kotlin.test.*

class NativeImageTest {
    @Test
    fun test() = suspendTest {
        val bmp = NativeImage(4, 4)
        bmp.setRgba(0, 0, Colors.RED)
        assertEquals(Colors.RED, bmp.getRgba(0, 0))
        bmp.setRgba(1, 0, Colors.BLUE)
        bmp.setRgba(1, 1, Colors.GREEN)
        //bmp.setRgba(0, 1, Colors.PINK)
        bmp.context2d {
            fillStyle = ColorPaint(Colors.PINK)
            fillRect(0, 1, 1, 1)
        }
        bmp.copy(0, 0, bmp, 2, 2, 2, 2)
        assertEquals(Colors.RED, bmp.getRgba(2, 2))
        assertEquals(Colors.BLUE, bmp.getRgba(3, 2))
        assertEquals(Colors.GREEN, bmp.getRgba(3, 3))
        assertEquals(Colors.PINK, bmp.getRgba(2, 3))
        //bmp.showImageAndWait()
    }
}
