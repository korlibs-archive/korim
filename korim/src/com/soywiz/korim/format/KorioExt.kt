package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsFile

fun Vfs.registerBitmapReading() {
	this.registerReadSpecial<Bitmap> { file, onProgress ->
		// @TODO: in html5 download and decode file using browser
		// if it fails (no codec supported), it will use ImageFormats instead
		// so it will work too.
		throw kotlin.UnsupportedOperationException()
	}
}

@Suppress("unused")
private val init = run {
	ResourcesVfs.vfs.registerBitmapReading()
}

suspend fun ImageFormat.decode(s: VfsFile) = asyncFun { this.read(s.readAsSyncStream()) }
suspend fun ImageFormat.decode(s: AsyncStream) = asyncFun { this.read(s.readAll()) }

suspend fun VfsFile.readBitmap(): Bitmap = asyncFun {
	try {
		this.readSpecial<Bitmap>()
	} catch (u: Throwable) {
		ImageFormats.decode(this)
	}
}
