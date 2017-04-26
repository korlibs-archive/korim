package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.util.Extra

open class ImageFrame(
	val bitmap: Bitmap,
	val time: Long = 0L,
	val targetX: Int = 0,
	val targetY: Int = 0,
	val main: Boolean = true
) : Extra by Extra.Mixin() {
	val area: Int get() = bitmap.area
}

val Iterable<ImageFrame>.area: Int get() = this.sumBy { it.area }
