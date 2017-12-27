package com.soywiz.korim.format

import com.soywiz.kds.Extra
import com.soywiz.korma.geom.Size

open class ImageInfo : Extra by Extra.Mixin() {
	var width: Int = 0
	var height: Int = 0
	var bitsPerPixel: Int = 0

	val size: Size get() = Size(width, height)

	override fun toString(): String = "ImageInfo(width=$width, height=$height, bpp=$bitsPerPixel, extra=$extra)"
}