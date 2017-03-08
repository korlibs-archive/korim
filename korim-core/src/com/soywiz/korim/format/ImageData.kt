package com.soywiz.korim.format

import com.soywiz.korio.util.Extra

open class ImageData(
		val frames: List<ImageFrame>
) : Extra by Extra.Mixin() {
	val mainBitmap get() = frames.sortedByDescending {
		if (it.main) {
			Int.MAX_VALUE
		} else {
			it.bitmap.width * it.bitmap.height * (it.bitmap.bpp * it.bitmap.bpp)
		}
	}.firstOrNull()?.bitmap ?: throw IllegalArgumentException("No bitmap found")
}