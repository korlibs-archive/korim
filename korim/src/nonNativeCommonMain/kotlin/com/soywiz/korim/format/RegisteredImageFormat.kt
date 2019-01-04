package com.soywiz.korim.format

import com.soywiz.korio.concurrent.atomic.*

//private var _RegisteredImageFormats_formats by korAtomic(ImageFormats(PNG))
private var _RegisteredImageFormats_formats = ImageFormats(PNG)

internal actual var RegisteredImageFormats_formats: ImageFormats
    get() = _RegisteredImageFormats_formats
    set(value) = run { _RegisteredImageFormats_formats = value }
