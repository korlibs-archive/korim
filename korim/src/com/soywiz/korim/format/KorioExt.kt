package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.vfs.VfsFile

@Suppress("unused")
private val init = run {
    // @TODO:
    //Vfs.registerReadSpecial
    //Vfs.registerWriteSpecial
    //ImageFormats.init
    //ResourcesVfs.registerReadSpecial(Bitmap)
    Unit
}

suspend fun ImageFormat.decode(s: VfsFile) = asyncFun { this.read(s.readAsSyncStream()) }
suspend fun ImageFormat.decode(s: AsyncStream) = asyncFun { this.read(s.readAll()) }

suspend fun VfsFile.readBitmap(): Bitmap = asyncFun {
    try {
        this.readSpecial<Bitmap>()
    } catch (u: UnsupportedOperationException) {
        ImageFormats.decode(this)
    }
}
