package com.soywiz.korim.color

object BGRA : ColorFormat32(), ColorFormatBase by ColorFormatBase.Mixin(
	bOffset = 0, bSize = 8,
	gOffset = 8, gSize = 8,
	rOffset = 16, rSize = 8,
	aOffset = 24, aSize = 8
)