package com.soywiz.korim.format

import com.soywiz.korio.util.Extra

data class ImageEncodingProps(
	val quality: Double = 0.81
) : Extra by Extra.Mixin()