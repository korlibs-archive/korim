package com.soywiz.korim.geom.binpack

import com.soywiz.korim.geom.Rectangle
import com.soywiz.korim.geom.Size
import com.soywiz.korim.geom.binpack.BinPack
import com.soywiz.korim.geom.binpack.MaxRects
import com.soywiz.korim.geom.binpack.addBatch
import com.soywiz.korio.error.invalidArgument

class BinPacker(val width: Double, val height: Double, val algo: BinPack = MaxRects(width, height)) {
	val allocated = arrayListOf<Rectangle>()

	fun add(width: Double, height: Double): Rectangle {
		val rect = algo.add(width, height) ?: invalidArgument("Size '${this.width}x${this.height}' doesn't fit in '${this.width}x${this.height}'")
		allocated += rect
		return rect
	}

	fun <T> addBatch(items: Iterable<T>, getSize: (T) -> Size): List<Pair<T, Rectangle?>> = algo.addBatch(items, getSize)
	fun addBatch(items: Iterable<Size>): List<Rectangle?> = algo.addBatch(items) { it }.map { it.second }
}
