package com.soywiz.korim.format

import com.soywiz.korio.stream.*
import kotlinx.atomicfu.*

object RegisteredImageFormats : ImageFormat() {
    private var _formats = atomic(ImageFormats(PNG))

    var formats: ImageFormats
        get() = _formats.value
        set(value) = run { _formats.value = value }

    fun register(vararg formats: ImageFormat) {
        this.formats = ImageFormats(this.formats.formats + formats)
    }

    fun unregister(vararg formats: ImageFormat) {
        this.formats = ImageFormats(this.formats.formats - formats)
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

    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData = formats.readImage(s, props)
    override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) = formats.writeImage(image, s, props)
    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = formats.decodeHeader(s, props)
    override fun toString(): String = "RegisteredImageFormats($formats)"
}

