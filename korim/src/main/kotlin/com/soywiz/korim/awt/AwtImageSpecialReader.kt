package com.soywiz.korim.awt

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsSpecialReader
import java.io.File

class AwtImageSpecialReader : VfsSpecialReader<NativeImage>(NativeImage::class) {
	override suspend fun readSpecial(vfs: Vfs, path: String): NativeImage {
		return when (vfs) {
			is LocalVfs -> {
				//println("LOCAL: AwtImageSpecialReader.readSpecial: $vfs, $path")
				AwtNativeImage(awtReadImageInWorker(File(path)))
			}
			else -> {
				//println("OTHER: AwtImageSpecialReader.readSpecial: $vfs, $path")
				AwtNativeImage(awtReadImageInWorker(vfs[path].readAll()))
			}
		}
	}
}