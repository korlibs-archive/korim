package com.soywiz.korim.geom.binpack

import com.soywiz.korim.geom.Rectangle
import com.soywiz.korim.geom.Size

fun <T> BinPack.addBatch(items: Iterable<T>, getSize: (T) -> Size): List<Pair<T, Rectangle?>> {
	val its = items.toList()
	val out = hashMapOf<T, Rectangle?>()
	val sorted = its.map { it to getSize(it) }.sortedByDescending { it.second.area }
	for ((i, size) in sorted) out[i] = this.add(size.width, size.height)
	return its.map { it to out[it] }
}