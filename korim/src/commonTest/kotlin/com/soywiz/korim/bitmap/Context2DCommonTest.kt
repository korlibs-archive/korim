package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class Context2DCommonTest {
    @Test
    fun testFillAlpha() = suspendTest({ !OS.isAndroid }) {
        val semiTransparentAlpha = Colors.FUCHSIA.withAd(0.5)
        val image = NativeImage(10, 10).context2d {
            fill(semiTransparentAlpha) {
                rect(-1, -1, 11, 11)
            }
        }.toBMP32().depremultipliedIfRequired()
        assertEquals(semiTransparentAlpha, image[5, 5])
    }
}
