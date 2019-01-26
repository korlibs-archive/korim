package com.soywiz.korim.format

import com.soywiz.korio.stream.*

private var _RegisteredImageFormats_formats = ImageFormats(PNG)

internal var RegisteredImageFormats_formats: ImageFormats
    get() = _RegisteredImageFormats_formats
    set(value) = run { _RegisteredImageFormats_formats = value }

object RegisteredImageFormats : ImageFormat() {
    var formats: ImageFormats
        get() = RegisteredImageFormats_formats
        set(value) = run { RegisteredImageFormats_formats = value }

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

