package com.soywiz.korim.bitmap.atlas

import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.binpack.BinPacker

// @TODO: Atlas building using BinPacking
class Atlas(val slices: List<BmpSlice>) {
	val slicesByName get() = slices.associateBy { it.name }

	val size: Int get() = slices.size
	operator fun get(name: String): BmpSlice = slicesByName[name]!!
	operator fun get(index: Int): BmpSlice = slices[index]!!
}

class MutableAtlas(val binPacker: BinPacker, val border: Int = 2, val premultiplied: Boolean = true) {
    constructor(width: Int, height: Int, border: Int = 2) : this(BinPacker(width, height), border)

    //val bitmap = NativeImage(binPacker.width.toInt(), binPacker.height.toInt(), premultiplied = premultiplied)
    val bitmap = Bitmap32(binPacker.width.toInt(), binPacker.height.toInt(), premultiplied = premultiplied)
    val slicesByIndex = arrayListOf<BmpSlice>()
    val slicesByName = LinkedHashMap<String, BmpSlice>()
    val size get() = slicesByIndex.size

    fun add(bmp: Bitmap, name: String = "Slice$size") = add(bmp.slice(), name)

    fun add(bmp: BmpSlice, name: String = "Slice$size"): BmpSlice {
        val rect = binPacker.add(bmp.width.toDouble() + border * 2, bmp.height.toDouble() + border * 2)
        val slice = this.bitmap.sliceWithSize((rect.left + border).toInt(), (rect.top + border).toInt(), bmp.width, bmp.height, name)
        bmp.bmp.copy(bmp.left, bmp.top, this.bitmap, slice.left, slice.top, slice.width, slice.height)
        slicesByIndex += slice
        slicesByName[name] = slice
        bitmap.contentVersion++
        return slice
    }

    // @TODO: We should copy the texture and regenerate the slices, just in case this is
    fun toImmutable() = Atlas(slicesByIndex.toList())
}
