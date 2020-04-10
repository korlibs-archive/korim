package com.soywiz.korim.font

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.ttf.readTtfFont
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test

class FontTest {
    @Test
    fun test() = suspendTest {
        //val font = resourcesVfs["tinymce-small.ttf"].readTtfFont()
        val font = SystemFont("Arial", 24)
        val image = NativeImage(128, 128).context2d {
            this.fillStyle = createColor(Colors.RED)
            this.font = font
            fillText("Hello World!", 16, 16)
        }
        image.showImageAndWait()
    }
}
