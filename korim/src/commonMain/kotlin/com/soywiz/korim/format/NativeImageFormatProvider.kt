package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.font.FontMetrics
import com.soywiz.korim.font.GlyphMetrics
import com.soywiz.korim.font.SystemFont
import com.soywiz.korim.vector.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.*
import kotlin.jvm.JvmOverloads
import kotlin.math.*

expect val nativeImageFormatProvider: NativeImageFormatProvider

data class NativeImageConfig(val premultiplied: Boolean)

abstract class NativeImageFormatProvider {
	abstract suspend fun decode(data: ByteArray, premultiplied: Boolean): NativeImage
    open suspend fun decode(vfs: Vfs, path: String, premultiplied: Boolean): NativeImage = decode(vfs.file(path).readBytes())
    suspend fun decode(file: FinalVfsFile, premultiplied: Boolean): Bitmap = decode(file.vfs, file.path, premultiplied)
	suspend fun decode(file: VfsFile, premultiplied: Boolean): Bitmap = decode(file.getUnderlyingUnscapedFile(), premultiplied)

    suspend fun decode(data: ByteArray): NativeImage = decode(data, premultiplied = true)
    suspend fun decode(vfs: Vfs, path: String): NativeImage = decode(vfs, path, premultiplied = true)
    suspend fun decode(file: FinalVfsFile): Bitmap = decode(file, premultiplied = true)
    suspend fun decode(file: VfsFile): Bitmap = decode(file, premultiplied = true)

    abstract suspend fun display(bitmap: Bitmap, kind: Int): Unit
	abstract fun create(width: Int, height: Int): NativeImage
	open fun copy(bmp: Bitmap): NativeImage = create(bmp.width, bmp.height).apply { context2d { drawImage(bmp, 0, 0) } }
	open fun mipmap(bmp: Bitmap, levels: Int): NativeImage = bmp.toBMP32().mipmap(levels).ensureNative()
	open fun mipmap(bmp: Bitmap): NativeImage {
        val out = NativeImage(ceil(bmp.width * 0.5).toInt(), ceil(bmp.height * 0.5).toInt())
        out.getContext2d(antialiasing = true).renderer.drawImage(bmp, 0, 0, out.width, out.height)
        return out
    }
}

suspend fun Bitmap.showImageAndWait(kind: Int = 0) = nativeImageFormatProvider.display(this, kind)
suspend fun ImageData.showImagesAndWait(kind: Int = 0) = run { for (frame in frames) frame.bitmap.showImageAndWait(kind) }
suspend fun SizedDrawable.showImageAndWait(kind: Int = 0) = this.render().toBMP32().showImageAndWait(kind)
