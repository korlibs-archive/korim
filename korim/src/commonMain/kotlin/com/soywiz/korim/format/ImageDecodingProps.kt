package com.soywiz.korim.format

import com.soywiz.kds.*

data class ImageDecodingProps(
	val filename: String = "unknown",
	val width: Int? = null,
	val height: Int? = null,
	override var extra: LinkedHashMap<String, Any?>? = null
) : Extra