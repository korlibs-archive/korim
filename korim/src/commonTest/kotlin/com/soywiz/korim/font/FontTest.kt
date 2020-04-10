package com.soywiz.korim.font

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.degrees
import kotlin.test.Ignore
import kotlin.test.Test

class FontTest {
    @Test
    @Ignore
    fun test() = suspendTest {
        BitmapFont(SystemFont("Arial"), 100.0, chars = CharacterSet.LATIN_ALL).register(name = "Arial")
        //BitmapFont(SystemFont("Arial"), 10.0, chars = CharacterSet.LATIN_ALL).register(name = "Arial")
        resourcesVfs["Comfortaa-Regular.ttf"].readTtfFont().register(name = "Arial")

        //val font =
        //val font = SystemFont("Arial")
        //SystemFontRegistry.register(BitmapFont(SystemFont("Arial"), 48.0, chars = CharacterSet.LATIN_ALL), "Arial")
        //BitmapFont()
        //val font = SystemFont("Arial", 24)
        //val font = SystemFont("Arial", 24)
        val image = NativeImage(512, 512).context2d {
        //val image = Bitmap32(512, 512).context2d {
            this.fillStyle = createLinearGradient(0, 0, 0, 48).add(0.0, Colors.BLUE).add(1.0, Colors.GREEN)
            //this.fillStyle = createColor(Colors.BLACK)
            this.fontName = "Arial"
            //this.font = font
            this.fontSize = 48.0
            translate(20, 20)
            rotate(15.degrees)
            fillText("12 Hello World!", 16, 48)
            //font.fillText(this, "HELLO", color = Colors.BLACK, x = 50.0, y = 50.0)
        }
        image.showImageAndWait()
    }
}
