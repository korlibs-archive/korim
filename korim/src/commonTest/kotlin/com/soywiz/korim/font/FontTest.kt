package com.soywiz.korim.font

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.paint.LinearGradientPaint
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.vector.*
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


        //DefaultTtfFont.renderGlyphToBitmap(256.0, '´'.toInt()).bmp.showImageAndWait()
        //DefaultTtfFont.renderGlyphToBitmap(256.0, 'o'.toInt(), nativeRendering = false).bmp.showImageAndWait()
        //DefaultTtfFont.renderGlyphToBitmap(256.0, 'ó'.toInt(), nativeRendering = false).bmp.showImageAndWait()
        //return@suspendTest

        //DefaultTtfFont.renderGlyphToBitmap(256.0, 'ó'.toInt(), nativeRendering = false).bmp.showImageAndWait()
        //DefaultTtfFont.renderGlyphToBitmap(256.0, 'ó'.toInt()).bmp.showImageAndWait()
        //println(result2)
        //result2.bmp.showImageAndWait()

        //BitmapFont(DefaultTtfFont, 64.0, paint = ColorPaint(Colors.RED)).atlas.showImageAndWait()
        //BitmapFont(SystemFont("Arial"), 64.0, paint = ColorPaint(Colors.RED)).atlas.showImageAndWait()

        Bitmap32(128, 128).context2d {
            //rect(0, 0, 50, 50)
            circle(25, 50, 30)
            clip()
            beginPath()

            moveTo(0, 100)
            lineTo(30, 10)
            lineTo(60, 100)
            close()
            fill()
        }.showImageAndWait()

        //val font = SystemFont("Arial")
        val font = DefaultTtfFont
        //val font = BitmapFont(DefaultTtfFont, 64.0)
        //val font = BitmapFont(DefaultTtfFont, 24.0)
        //val font = BitmapFont(SystemFont("Arial"), 24.0)
        //val font = SystemFont("Arial")
        //val font = BitmapFont(DefaultTtfFont, 24.0)

        //println(buildSvgXml { drawText("Hello World!") }.toString())
        //font.atlas.showImageAndWait()
        //val paint = ColorPaint(Colors.RED)
        //val paint = ColorPaint(Colors.BLUE)
        val paint = LinearGradientPaint(0, 0, 0, -48).add(0.0, Colors.BLUE).add(1.0, Colors.GREEN)
        //NativeImage(100, 100).context2d {
        //    fillStyle = paint
        //    fillRect(0, 0, 100, 100)
        //}.showImageAndWait()

        //val result = font.renderTextToBitmap(48.0, "Helló World!", paint, nativeRendering = false, renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
        val result = font.renderTextToBitmap(48.0, "Helló World!", paint, nativeRendering = false, renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
        //val result = font.renderTextToBitmap(24.0, "llll", ColorPaint(Colors.RED), renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
        //val result = font.renderTextToBitmap(24.0, "Hello World!", renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
            //dy = -n.toDouble()
            val scale = 1.0 + n * 0.1
            //val scale = 1.0
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
        assertEquals("FontMetrics(size=16, top=15, ascent=15, baseline=0, descent=-3, bottom=-3, leading=0, emHeight=18, lineHeight=18)", fmetrics.toString())
        val gmetrics = font.getGlyphMetrics(16.0, 'k'.toInt())
        assertEquals("GlyphMetrics(codePoint=107 ('k'), existing=true, xadvance=7, bounds=Rectangle(x=0, y=0, width=6, height=10))", gmetrics.toString())
    }

    @Test
    fun testReadFont() = suspendTest {
        val font1 = resourcesVfs["myfont.ttf"].readTtfFont(preload = true)
        val font2 = resourcesVfs["myfont-bug.ttf"].readTtfFont(preload = true)
        val font3 = resourcesVfs["myfont-bug2.ttf"].readTtfFont(preload = true)
        val font4 = resourcesVfs["myfont-bug3.ttf"].readTtfFont(preload = true)
        //font1.renderTextToBitmap(20.0, "Hello World!", border = 64, nativeRendering = false).bmp.showImageAndWait()
        //font4.renderTextToBitmap(64.0, "12 Hello World", nativeRendering = true).bmp.showImageAndWait()
    }
}
