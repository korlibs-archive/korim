package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import kotlin.test.*

class DDSTest : BaseImageFormatTest() {
	val formats = ImageFormats(PNG, DDS)
	val ResourcesVfs = root

	@kotlin.test.Test
	fun dxt1() = suspendTest {
		val output = ResourcesVfs["dxt1.dds"].readBitmapNoNative(formats)
		val expected = ResourcesVfs["dxt1.png"].readBitmapNoNative(formats)
		assertEquals(0, output.matchContentsDistinctCount(expected))
	}

	@kotlin.test.Test
	fun dxt3() = suspendTest {
		val output = ResourcesVfs["dxt3.dds"].readBitmapNoNative(formats)
		val expected = ResourcesVfs["dxt3.png"].readBitmapNoNative(formats)
		assertEquals(0, output.matchContentsDistinctCount(expected))
		//output.writeTo(LocalVfs("c:/temp/dxt3.png"))
	}

	@kotlin.test.Test
	fun dxt5() = suspendTest {
		val output = ResourcesVfs["dxt5.dds"].readBitmapNoNative(formats)
		val expected = ResourcesVfs["dxt5.png"].readBitmapNoNative(formats)
		assertEquals(0, output.matchContentsDistinctCount(expected))
		//output.writeTo(LocalVfs("c:/temp/dxt5.png"))
	}

    @kotlin.test.Test
    fun dxt5_registered() = suspendTest {
        RegisteredImageFormats.temporalRegister(DDS, PNG) {
            val output = ResourcesVfs["dxt5.dds"].readBitmapNoNative()
            val expected = ResourcesVfs["dxt5.png"].readBitmapNoNative()
            assertEquals(0, output.matchContentsDistinctCount(expected))
            //output.writeTo(LocalVfs("c:/temp/dxt5.png"))
        }
    }
}
