package com.soywiz.kimage.format

import com.soywiz.kimage.bitmap.Bitmap
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.*
import com.soywiz.korio.vfs.VfsFile
import java.io.File

open class ImageFormat {
    open fun check(s: SyncStream): Boolean = TODO()
    open fun read(s: SyncStream): Bitmap = TODO()
    fun read(file: File) = this.read(file.openSync())
    fun read(s: ByteArray): Bitmap = read(s.openSync())
    open fun write(bitmap: Bitmap, s: SyncStream): Unit = TODO()

    suspend fun decode(s: VfsFile) = asyncFun { this.read(s.readAsSyncStream()) }
    suspend fun decode(s: AsyncStream) = asyncFun { this.read(s.readAll()) }

    fun decode(s: SyncStream) = this.read(s)
    fun decode(file: File) = this.read(file.openSync("r"))
    fun decode(s: ByteArray): Bitmap = read(s.openSync())

    fun encode(bitmap: Bitmap): ByteArray {
        val mem = MemorySyncStream(byteArrayOf())
        write(bitmap, mem)
        return mem.toByteArray()
    }
}

object ImageFormats : ImageFormat() {
    private val formats = listOf(PNG, JPEG, BMP, TGA)

    override fun check(s: SyncStream): Boolean {
        for (format in formats) if (format.check(s.slice())) return true
        return false
    }

    override fun read(s: SyncStream): Bitmap {
        for (format in formats) {
            if (format.check(s.slice())) {
                return format.read(s.slice())
            }
        }
        throw UnsupportedOperationException("Not suitable image format")
    }
}

suspend fun VfsFile.readBitmap(): Bitmap = asyncFun {
    try {
        this.readSpecial<Bitmap>()
    } catch (u: UnsupportedOperationException) {
        ImageFormats.decode(this)
    }
}
