package com.soywiz.korim.format

import com.soywiz.korio.util.*
import org.w3c.dom.*
import kotlin.browser.*

object HtmlCanvas {
	fun createCanvas(width: Int, height: Int): HTMLCanvasElement {
		if (OS.isNodejs) {
			return js("new (require('canvas'))(width, height)").unsafeCast<HTMLCanvasElement>()
		} else {
			val out = document.createElement("canvas").unsafeCast<HTMLCanvasElement>()
			out.width = width
			out.height = height
			return out
		}
	}
}
