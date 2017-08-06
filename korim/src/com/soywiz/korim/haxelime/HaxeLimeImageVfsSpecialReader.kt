package com.soywiz.korim.haxelime

import com.jtransc.JTranscSystem
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korio.vfs.VfsSpecialReader

class HaxeLimeImageVfsSpecialReader : VfsSpecialReader<NativeImage>(NativeImage::class.java) {
	override val available: Boolean = JTranscSystem.isHaxe()
}