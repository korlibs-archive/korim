package com.soywiz.korim.format

val StandardImageFormats = listOf(JPEG, PNG, TGA, PSD)

fun ImageFormats.registerStandard() = this.apply { register(StandardImageFormats) }