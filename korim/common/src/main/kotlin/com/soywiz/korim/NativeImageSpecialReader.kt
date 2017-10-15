package com.soywiz.korim

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korio.vfs.VfsSpecialReader

expect object NativeImageSpecialReader {
	val instance: VfsSpecialReader<NativeImage>
}
