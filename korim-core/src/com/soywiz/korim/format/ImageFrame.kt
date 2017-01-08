package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap

open class ImageFrame(
	val bitmap: Bitmap,
	val time: Long = 0L,
	val targetX: Int = 0,
	val targetY: Int = 0,
	val extra: Any? = null
)
