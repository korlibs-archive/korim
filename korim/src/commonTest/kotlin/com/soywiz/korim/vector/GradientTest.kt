package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import kotlin.test.*

class GradientTest {
    @Test
    fun test() = suspendTest {
        //NativeImageOrBitmap32(512, 512, true).context2d {
        NativeImageOrBitmap32(512, 512, false).context2d {
            fillStyle = createRadialGradient(100, 100, 20, 150, 100, 100, CycleMethod.REFLECT).addColorStop(0, Colors.RED).addColorStop(1, Colors.TRANSPARENT_BLACK)
            //fillStyle = createSweepGradient(100, 100).addColorStop(0, Colors.RED).addColorStop(1, Colors.DARKGREEN)
            fillRect(0, 0, 512, 512)
        }
        //.showImageAndWait()
    }
}
