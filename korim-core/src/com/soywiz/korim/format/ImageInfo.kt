package com.soywiz.korim.format

import com.soywiz.korio.util.Extra

class ImageInfo : Extra by Extra.Mixin() {
	var width: Int = 0
	var height: Int = 0
	var bitsPerPixel: Int = 0
}