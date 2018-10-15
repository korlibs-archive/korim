package com.soywiz.korim.format

import kotlinx.atomicfu.*

private var _RegisteredImageFormats_formats = atomic(ImageFormats(PNG))

internal actual var RegisteredImageFormats_formats: ImageFormats
    get() = _RegisteredImageFormats_formats.value
    set(value) = run { _RegisteredImageFormats_formats.value = value }
