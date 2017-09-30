package com.soywiz.korim.format

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.UrlVfs
import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsSpecialReader

class HtmlImageSpecialReader : VfsSpecialReader<NativeImage>(NativeImage::class) {
	//override val available: Boolean = OS.isJs

	override suspend fun readSpecial(vfs: Vfs, path: String): NativeImage {
		val canvas = when (vfs) {
			is LocalVfs -> {
				//println("LOCAL: HtmlImageSpecialReader: $vfs, $path")
				NativeImageFormatProvider.BrowserImage.loadImage(path)
			}
			is UrlVfs -> {
				//println("URL: HtmlImageSpecialReader: $vfs, $path")
				NativeImageFormatProvider.BrowserImage.loadImage(vfs.getFullUrl(path))
			}
			else -> {
				//println("OTHER: HtmlImageSpecialReader: $vfs, $path")
				NativeImageFormatProvider.BrowserImage.decodeToCanvas(vfs[path].readAll())
			}
		}
		return CanvasNativeImage(canvas)
	}
}