package com.soywiz.korim.bitmap

class Atlas(val slices: List<BmpSlice>) {
	val slicesByName get() = slices.associateBy { it.name }

	val size: Int get() = slices.size
	operator fun get(name: String): BmpSlice = slicesByName[name]!!
	operator fun get(index: Int): BmpSlice = slices[index]!!
}
