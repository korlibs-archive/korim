package com.soywiz.korim.format

val StandardImageFormats = listOf(JPEG, PNG, TGA)

fun ImageFormats.registerStandard() = this.apply { register(StandardImageFormats) }