package com.soywiz.korim.format

import com.soywiz.korio.util.Extra

data class ImageEncodingProps(
		val filename: String = "",
		val quality: Double = 0.81,
		override var extra: HashMap<String, Any?>? = null
) : Extra
