package com.soywiz.korim.geom.binpack

import com.soywiz.korim.geom.Rectangle

interface BinPack {
	val maxWidth: Double
	val maxHeight: Double
	fun add(width: Double, height: Double): Rectangle?
}