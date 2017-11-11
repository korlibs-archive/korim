package com.soywiz.korim.format

import com.soywiz.kds.Extra
import com.soywiz.korim.bitmap.Bitmap

open class ImageFrame(
	val bitmap: Bitmap,
	val time: Long = 0L,
	val targetX: Int = 0,
	val targetY: Int = 0,
	val main: Boolean = true
) : Extra by Extra.Mixin() {
	val area: Int get() = bitmap.area

	override fun toString(): String = "ImageFrame($bitmap, time=$time, targetX=$targetX, targetY=$targetY, main=$main)"
}

val Iterable<ImageFrame>.area: Int get() = this.sumBy { it.area }
