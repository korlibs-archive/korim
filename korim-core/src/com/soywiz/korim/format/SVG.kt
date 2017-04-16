package com.soywiz.korim.format

import com.soywiz.korim.vector.render
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.stream.readString
import com.soywiz.korio.stream.slice

class SVG : ImageFormat("svg") {
	override fun decodeHeader(s: SyncStream, filename: String): ImageInfo? {
		val start = s.slice().readString(Math.min(100, s.length.toInt())).trim().toLowerCase()
		try {
			if (start.startsWith("<svg") || start.startsWith("<?xml")) {
				val content = s.slice().readAll().toString(Charsets.UTF_8).trim()
				val svg = com.soywiz.korim.vector.format.SVG(content)
				return ImageInfo().apply {
					width = svg.width
					height = svg.height
				}
			} else {
				return null
			}
		} catch (t: Throwable) {
			return null
		}
	}

	override fun readImage(s: SyncStream, filename: String): ImageData {
		val content = s.slice().readAll().toString(Charsets.UTF_8).trim()
		val svg = com.soywiz.korim.vector.format.SVG(content)
		return ImageData(listOf(ImageFrame(
			svg.render().toBmp32()
		)))
	}
}