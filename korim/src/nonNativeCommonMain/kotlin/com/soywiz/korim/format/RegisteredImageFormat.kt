package com.soywiz.korim.format

import com.soywiz.korio.atomic.*

private var _RegisteredImageFormats_formats = korAtomic(ImageFormats(PNG))

internal actual var RegisteredImageFormats_formats: ImageFormats
    get() = _RegisteredImageFormats_formats.value
    set(value) = run { _RegisteredImageFormats_formats.value = value }
