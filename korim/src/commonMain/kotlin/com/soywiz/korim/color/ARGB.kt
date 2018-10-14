package com.soywiz.korim.color

object ARGB : ColorFormat32(), ColorFormatBase by ColorFormatBase.Mixin(
	bOffset = 24, bSize = 8,
	gOffset = 16, gSize = 8,
	rOffset = 8, rSize = 8,
	aOffset = 0, aSize = 8
)
