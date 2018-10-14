package com.soywiz.korim.format

import com.soywiz.korim.vector.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.math.*

object SVG : ImageFormat("svg") {
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		val start = s.sliceStart().readString(min(100, s.length.toInt())).trim().toLowerCase()
		return try {
			if (start.startsWith("<svg") || start.startsWith("<?xml")) {
				val content = s.sliceStart().readAll().toString(UTF8).trim()
				val svg = com.soywiz.korim.vector.format.SVG(content)
				ImageInfo().apply {
					width = svg.width
					height = svg.height
				}
			} else {
				null
			}
		} catch (t: Throwable) {
			null
		}
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val content = s.sliceStart().readAll().toString(UTF8).trim()
		val svg = com.soywiz.korim.vector.format.SVG(content)
		return ImageData(listOf(ImageFrame(svg.render().toBmp32())))
	}
}