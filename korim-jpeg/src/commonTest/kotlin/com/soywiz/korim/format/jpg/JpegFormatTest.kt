package com.soywiz.korim.format.jpg

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlin.test.*

class JpegFormatTest {
    val MyResourcesVfs = when {
        OS.isJs -> localCurrentDirVfs["src/commonTest/resources"]
        OS.isNative -> localCurrentDirVfs["../../../../../../src/commonTest/resources"]
        else -> ResourcesVfs
    }

    val formats = ImageFormats(JPEG, PNG)

    lateinit var root: VfsFile

    inline fun imgTest(crossinline callback: suspend () -> Unit) = suspendTest { // @TODO: Generation bug
    //inline fun imgTest(noinline callback: suspend () -> Unit) = suspendTest {
        for (path in listOf(applicationVfs["src/test/resources"], MyResourcesVfs)) {
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
    @Ignore
    fun jpegNative() = imgTest {
        val bitmap = MyResourcesVfs["kotlin.jpg"].readBitmap(formats = formats)
        //assertTrue(bitmap is NativeImage)
        assertEquals("Bitmap32(190, 190)", bitmap.toBMP32().toString())

        val bitmapExpected = MyResourcesVfs["kotlin.jpg.png"].readBitmap(formats = formats)
        assertTrue(Bitmap32.matches(bitmapExpected, bitmap))

        //val diff = Bitmap32.diff(bitmapExpected, bitmap)
        //diff.transformColor { RGBA.pack(RGBA.getR(it) * 0xFF, RGBA.getG(it) * 0xFF, RGBA.getB(it) * 0xFF, 0xFF) }
        //awtShowImage(diff); Thread.sleep(10000L)
    }

    @Test
    @Ignore
    fun jpeg2Native() = imgTest {
        val bitmap = MyResourcesVfs["img1.jpg"].readBitmap(formats = formats)
        //assertTrue(bitmap is NativeImage)
        assertEquals("Bitmap32(460, 460)", bitmap.toBMP32().toString())

        val bitmapExpected = MyResourcesVfs["img1.jpg.png"].readBitmap(formats = formats)
        assertTrue(Bitmap32.matches(bitmapExpected, bitmap, threshold = 32))

        //val diff = Bitmap32.diff(bitmapExpected, bitmap)
        //diff.transformColor { RGBA.pack(RGBA.getR(it) * 4, RGBA.getG(it) * 4, RGBA.getB(it) * 4, 0xFF) }
        //diff.transformColor { RGBA.pack(RGBA.getR(it) * 0xFF, RGBA.getG(it) * 0xFF, RGBA.getB(it) * 0xFF, 0xFF) }
        //awtShowImage(diff); Thread.sleep(10000L)
    }

    @Test
    fun jpegEncoder() = imgTest {
        val bitmapOriginal = root["kotlin32.png"].readBitmapNoNative(formats).toBMP32()
        val bytes = JPEG.encode(bitmapOriginal, ImageEncodingProps(quality = 0.5))
        //val bitmapOriginal = LocalVfs("/tmp/aa.jpg").readBitmapNoNative().toBMP32()
        //bitmapOriginal.writeTo(LocalVfs("/tmp/out.jpg"))
    }

    @Test
    fun ajpeg() = imgTest {
        val bitmap = root["kotlin.jpg"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(190, 190)", bitmap.toString())
    }

    @Test
    fun ajpeg2() = imgTest {
        val bitmap = root["img1.jpg"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(460, 460)", bitmap.toString())
    }
}
