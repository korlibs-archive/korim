package com.soywiz.korim.format

import com.soywiz.kds.*

data class ImageEncodingProps(
	val filename: String = "",
	val quality: Double = 0.81,
	override var extra: LinkedHashMap<String, Any?>? = null
) : Extra
