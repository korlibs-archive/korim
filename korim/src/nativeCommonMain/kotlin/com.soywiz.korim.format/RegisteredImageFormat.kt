package com.soywiz.korim.format

private var _RegisteredImageFormats_formats = ImageFormats(PNG)

internal actual var RegisteredImageFormats_formats: ImageFormats
    get() = _RegisteredImageFormats_formats
    set(value) = run { _RegisteredImageFormats_formats = value }


/*
// @TODO: atomic doesn't seems to work (don't know why)
@ThreadLocal
actual var RegisteredImageFormats_formats: ImageFormats = ImageFormats(PNG)
*/
