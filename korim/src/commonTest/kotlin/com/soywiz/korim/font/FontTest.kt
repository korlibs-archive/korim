package com.soywiz.korim.font

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Ignore
import kotlin.test.Test

class FontTest {
    @Test
    @Ignore
    fun test() = suspendTest {
        val font = resourcesVfs["tinymce-small.ttf"].readTtfFont()
        //val font = SystemFont("Arial", 24)
        //val font = BitmapFont(SystemFont("Arial", 24))
        //val font = SystemFont("Arial", 24)
        val image = NativeImage(128, 128).context2d {
            this.fillStyle = createLinearGradient(0, 0, 0, 24).add(0.0, Colors.BLUE).add(1.0, Colors.GREEN)
            //this.fillStyle = createColor(Colors.BLACK)
            this.font = font
            fillText("Hello World!", 16, 16)
        }
        image.showImageAndWait()
    }
}
