package com.soywiz.korim.format

import com.soywiz.korio.util.Extra
import com.soywiz.korma.geom.Size

class ImageInfo : Extra by Extra.Mixin() {
	var width: Int = 0
	var height: Int = 0
	var bitsPerPixel: Int = 0

	val size: Size get() = Size(width, height)
}