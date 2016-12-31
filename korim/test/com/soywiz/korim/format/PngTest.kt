package com.soywiz.korim.format

import com.soywiz.korio.async.sync
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Assert
import org.junit.Test

class PngTest {
    @Test
    fun readBitmap() = sync {
        val bitmap = ResourcesVfs["kotlin.png"].readBitmap()
        Assert.assertEquals("Bitmap32(190, 190)", bitmap.toString())
    }
}