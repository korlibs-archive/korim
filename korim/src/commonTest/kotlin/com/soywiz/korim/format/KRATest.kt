package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class KRATest {
    val formats = ImageFormats(KRA)

    @Test
    fun kraTest() = suspendTestNoBrowser {
        val output = resourcesVfs["krita1.kra"].readBitmapNoNative(formats)
        val expected = resourcesVfs["krita1.kra.png"].readBitmapNoNative(formats)
        //showImageAndWait(output)
        assertEquals(0, output.matchContentsDistinctCount(expected))
    }
}
