package com.soywiz.korim.format

import com.soywiz.korio.util.Extra

data class ImageDecodingProps(
		val filename: String = "unknown",
		val width: Int? = null,
		val height: Int? = null,
		override var extra: HashMap<String, Any?>? = null
) : Extra