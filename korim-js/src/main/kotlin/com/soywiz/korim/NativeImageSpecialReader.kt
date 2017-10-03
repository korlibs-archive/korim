package com.soywiz.korim

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.format.HtmlImageSpecialReader
import com.soywiz.korio.vfs.VfsSpecialReader

actual object NativeImageSpecialReader {
	actual val instance: VfsSpecialReader<NativeImage> by lazy { HtmlImageSpecialReader() }
}
