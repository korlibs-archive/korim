package com.soywiz.korim.format.jpg

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class JpegFormatTest {
    val formats = ImageFormats(JPEG, PNG)

    lateinit var root: VfsFile

    fun imgTest(callback: suspend () -> Unit) = suspendTest {
        for (path in listOf(applicationVfs["src/test/resources"], ResourcesVfs)) {
            root = path
            if (root["kotlin8.png"].exists()) break
        }
        callback()
    }


    @Test
    fun jpeg() = imgTest {
        val bitmap = root["kotlin.jpg"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(190, 190)", bitmap.toString())
        //bitmap.writeTo(LocalVfs("c:/temp/img1.jpg.png"), formats = formats)
    }

    @Test
    fun jpeg2() = imgTest {
        val bitmap = root["img1.jpg"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(460, 460)", bitmap.toString())
        //bitmap.writeTo(LocalVfs("c:/temp/img1.jpg.tga"), formats = formats)
    }

    @Test
    fun jpegNative() = suspendTest {
        val bitmap = ResourcesVfs["kotlin.jpg"].readBitmap(formats = formats)
        assertEquals("AwtNativeImage(190, 190)", bitmap.toString())

        val bitmapExpected = ResourcesVfs["kotlin.jpg.png"].readBitmap(formats = formats)
        assertTrue(Bitmap32.matches(bitmapExpected, bitmap))

        //val diff = Bitmap32.diff(bitmapExpected, bitmap)
        //diff.transformColor { RGBA.pack(RGBA.getR(it) * 0xFF, RGBA.getG(it) * 0xFF, RGBA.getB(it) * 0xFF, 0xFF) }
        //awtShowImage(diff); Thread.sleep(10000L)
    }

    @Test
    fun jpeg2Native() = suspendTest {
        val bitmap = ResourcesVfs["img1.jpg"].readBitmap(formats = formats)
        assertEquals("AwtNativeImage(460, 460)", bitmap.toString())

        val bitmapExpected = ResourcesVfs["img1.jpg.png"].readBitmap(formats = formats)
        assertTrue(Bitmap32.matches(bitmapExpected, bitmap, threshold = 32))

        //val diff = Bitmap32.diff(bitmapExpected, bitmap)
        //diff.transformColor { RGBA.pack(RGBA.getR(it) * 4, RGBA.getG(it) * 4, RGBA.getB(it) * 4, 0xFF) }
        //diff.transformColor { RGBA.pack(RGBA.getR(it) * 0xFF, RGBA.getG(it) * 0xFF, RGBA.getB(it) * 0xFF, 0xFF) }
        //awtShowImage(diff); Thread.sleep(10000L)
    }

    @Test
    fun jpegEncoder() = suspendTest {
        val bitmapOriginal = root["kotlin32.png"].readBitmapNoNative(formats).toBMP32()
        val bytes = JPEG.encode(bitmapOriginal, ImageEncodingProps(quality = 0.5))
        //val bitmapOriginal = LocalVfs("/tmp/aa.jpg").readBitmapNoNative().toBMP32()
        //bitmapOriginal.writeTo(LocalVfs("/tmp/out.jpg"))
    }

    @Test
    fun ajpeg() = suspendTest {
        val bitmap = root["kotlin.jpg"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(190, 190)", bitmap.toString())
    }

    @Test
    fun ajpeg2() = suspendTest {
        val bitmap = root["img1.jpg"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(460, 460)", bitmap.toString())
    }
}
