package com.soywiz.korim.font

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korim.vector.buildSvgXml
import com.soywiz.korim.vector.paint.ColorPaint
import com.soywiz.korim.vector.paint.DefaultPaint
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.degrees
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class FontTest {
    @Test
    @Ignore
    fun test() = suspendTest {
        BitmapFont(SystemFont("Arial"), 100.0, chars = CharacterSet.LATIN_ALL).register(name = "Arial")
        //BitmapFont(SystemFont("Arial"), 10.0, chars = CharacterSet.LATIN_ALL).register(name = "Arial")
        //resourcesVfs["tinymce-small.ttf"].readTtfFont().register(name = "Arial")
        //BitmapFont(resourcesVfs["chunky-wally.ttf"].readTtfFont(), 100.0).register(name = "Arial") // @TODO: This doesn't work probably because bounds are not right
        resourcesVfs["chunky-wally.ttf"].readTtfFont().register(name = "Arial")
        //resourcesVfs["Comfortaa-Regular.ttf"].readTtfFont().register(name = "Arial")
        //resourcesVfs["OptimusPrinceps.ttf"].readTtfFont().register(name = "Arial")

        println(buildSvgXml {
            this.fillStyle = createLinearGradient(0, 0, 0, 48).add(0.0, Colors.BLUE).add(1.0, Colors.GREEN)
            //this.fillStyle = createColor(Colors.BLACK)
            this.fontName = "Arial"
            //this.font = font
            this.fontSize = 48.0
            translate(20, 20)
            rotate(15.degrees)
            fillText("12 Hello World!", 16, 48)
        })

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

    @Test
    @Ignore
    fun test2() = suspendTest {
        //val font = DefaultTtfFont

        //DefaultTtfFont.renderGlyphToBitmap(64.0, 'l'.toInt()).bmp.showImageAndWait()
        //println(result2)
        //result2.bmp.showImageAndWait()

        //BitmapFont(DefaultTtfFont, 64.0, CharacterSet("l")).atlas.showImageAndWait()

        //val font = DefaultTtfFont
        val font = BitmapFont(DefaultTtfFont, 24.0)
        //val font = BitmapFont(DefaultTtfFont, 24.0)

        //println(buildSvgXml { drawText("Hello World!") }.toString())
        //font.atlas.showImageAndWait()
        val result = font.renderTextToBitmap(24.0, "Hello World!", ColorPaint(Colors.RED), renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
        //val result = font.renderTextToBitmap(24.0, "llll", ColorPaint(Colors.RED), renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
        //val result = font.renderTextToBitmap(24.0, "Hello World!", renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
            //dy = -n.toDouble()
            val scale = 1.0 + n * 0.1
            //transform.translate(0.0, scale)
            transform.scale(scale)
            transform.rotate(25.degrees)
            put(c)
            advance(advance * scale)
        })
        result.bmp.showImageAndWait()
    }

    @Test
    fun testDefaultFont() {
        val font = DefaultTtfFont
        val fmetrics = font.getFontMetrics(16.0)
        assertEquals("FontMetrics(size=16, top=15, ascent=15, baseline=0, descent=-3, bottom=-3, leading=0, lineHeight=18)", fmetrics.toString())
        val gmetrics = font.getGlyphMetrics(16.0, 'k'.toInt())
        assertEquals("GlyphMetrics(codePoint=107 ('k'), existing=true, xadvance=7, bounds=Rectangle(x=0, y=0, width=6, height=10))", gmetrics.toString())
    }
}
