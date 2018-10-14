package com.soywiz.korim.format

import kotlinx.atomicfu.*

private var _RegisteredImageFormats_formats = atomic(ImageFormats(PNG))

actual var RegisteredImageFormats_formats: ImageFormats
    get() = _RegisteredImageFormats_formats.value
    set(value) = run { _RegisteredImageFormats_formats.value = value }


/*
// @TODO: atomic doesn't seems to work (don't know why)
@ThreadLocal
actual var RegisteredImageFormats_formats: ImageFormats = ImageFormats(PNG)
*/
