package com.soywiz.korim.format

import com.soywiz.korio.stream.*
import kotlinx.atomicfu.*

object RegisteredImageFormats : ImageFormat() {
    var formats = atomic(ImageFormats(listOf()))

    fun register(vararg formats: ImageFormat) {
        this.formats.value = ImageFormats(this.formats.value.formats + formats)
    }

    inline fun <T> temporalRegister(vararg formats: ImageFormat, callback: () -> T): T {
        val oldFormats = this.formats
        try {
            register(*formats)
            return callback()
        } finally {
            this.formats = oldFormats
        }
    }

    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData = formats.value.readImage(s, props)
    override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) = formats.value.writeImage(image, s, props)
    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = formats.value.decodeHeader(s, props)
    override fun toString(): String = "RegisteredImageFormats(${formats.value})"
}

