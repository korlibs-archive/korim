package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.*

expect val nativeImageFormatProvider: NativeImageFormatProvider

abstract class NativeImageFormatProvider {
	abstract suspend fun decode(data: ByteArray): NativeImage
	open suspend fun decode(vfs: Vfs, path: String): NativeImage = decode(vfs.file(path).readBytes())
	suspend fun decode(file: FinalVfsFile): Bitmap = decode(file.vfs, file.path)
	suspend fun decode(file: VfsFile): Bitmap = decode(file.getUnderlyingUnscapedFile())
	abstract suspend fun display(bitmap: Bitmap, kind: Int): Unit

	abstract fun create(width: Int, height: Int): NativeImage
	open fun copy(bmp: Bitmap): NativeImage = create(bmp.width, bmp.height).apply { context2d { drawImage(bmp, 0, 0) } }
	abstract fun mipmap(bmp: Bitmap, levels: Int): NativeImage
	abstract fun mipmap(bmp: Bitmap): NativeImage
}
