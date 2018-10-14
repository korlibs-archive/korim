package com.soywiz.korim.format

import com.soywiz.kds.*

open class ImageData(
	val frames: List<ImageFrame>
) : Extra by Extra.Mixin() {
	val area: Int get() = frames.area

	val mainBitmap
		get() = frames.sortedBy2 {
			if (it.main) {
				Int.MAX_VALUE
			} else {
				it.bitmap.width * it.bitmap.height * (it.bitmap.bpp * it.bitmap.bpp)
			}
		}.firstOrNull()?.bitmap ?: throw IllegalArgumentException("No bitmap found")

	override fun toString(): String = "ImageData($frames)"
}