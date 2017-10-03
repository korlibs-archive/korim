package com.soywiz.korim

import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsSpecialReader

actual object NativeImageSpecialReader {
	actual val instance: VfsSpecialReader<NativeImage> by lazy { AndroidImageSpecialReader() }
}

class AndroidImageSpecialReader : VfsSpecialReader<NativeImage>(NativeImage::class) {
	override suspend fun readSpecial(vfs: Vfs, path: String): NativeImage {
		return when (vfs) {
			is LocalVfs -> {
				//println("LOCAL: AwtImageSpecialReader.readSpecial: $vfs, $path")
				//AwtNativeImage(awtReadImageInWorker(File(path)))
				TODO()
			}
			else -> {
				//println("OTHER: AwtImageSpecialReader.readSpecial: $vfs, $path")
				//AwtNativeImage(awtReadImageInWorker(vfs[path].readAll()))
				TODO()
			}
		}
	}
}