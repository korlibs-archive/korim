package com.soywiz.korim.format

import com.jtransc.annotation.JTranscMethodBody
import com.jtransc.js.*
import com.soywiz.korim.awt.awtReadImage
import com.soywiz.korim.awt.toBMP32
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.html.HtmlImage
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korio.vfs.js.jsObject
import kotlin.coroutines.CoroutineIntrinsics
import kotlin.coroutines.suspendCoroutine

@JTranscMethodBody(target = "js", value = """return {% SMETHOD com.soywiz.korim.format.BrowserImage:gen %}(p0, p1);""")
private suspend fun gen(bytes: ByteArray): Bitmap = asyncFun { AwtImage.gen(bytes) }

suspend fun ImageFormat.decode(s: VfsFile) = asyncFun { this.read(s.readAsSyncStream()) }
suspend fun ImageFormat.decode(s: AsyncStream) = asyncFun { this.read(s.readAll()) }

suspend fun VfsFile.readBitmap(): Bitmap = asyncFun {
	try {
		val bytes = this.read()
		com.soywiz.korim.format.gen(bytes)
	} catch (t: Throwable) {
		t.printStackTrace()
		ImageFormats.decode(this)
	}
}

@Suppress("unused")
object BrowserImage {
	suspend fun loadjsimg(bytes: ByteArray): JsDynamic? = suspendCoroutine { continuation ->
		val blob = jsNew("Blob", jsArray(bytes), jsObject("type" to "image/png"))
		val blobURL = global["URL"].methods["createObjectURL"](blob);

		val img = jsNew("Image")
		img["onload"] = jsFunctionRaw0 {
			val canvas = document.methods["createElement"]("canvas");
			canvas["width"] = img["width"]
			canvas["height"] = img["height"]
			val ctx = canvas.methods["getContext"]("2d");
			ctx.methods["drawImage"](img, 0, 0);
			global["URL"].methods["revokeObjectURL"](blobURL);
			continuation.resume(canvas)
		};
		img["onerror"] = jsFunctionRaw0 {
			continuation.resumeWithException(RuntimeException("error loading image"))
		};
		img["src"] = blobURL;
	}

	fun imgData(canvas: JsDynamic?, out: IntArray): Unit {
		HtmlImage.renderHtmlCanvasIntoBitmap(canvas, out)
	}

	@JvmStatic suspend fun gen(bytes: ByteArray): Bitmap = asyncFun {
		val img = BrowserImage.loadjsimg(bytes)
		val width = img["width"].toInt()
		val height = img["height"].toInt()
		val data = kotlin.IntArray(width * height)
		BrowserImage.imgData(img, data)
		Bitmap32(width, height, data)
	}

	@Suppress("unused")
	private fun getSuspended() = CoroutineIntrinsics.SUSPENDED
}

object AwtImage {
	@JvmStatic suspend fun gen(bytes: ByteArray): Bitmap = asyncFun {
		try {
			//println("AwtImage.gen!")
			awtReadImage(bytes).toBMP32()
		} catch (t: Throwable) {
			//t.printStackTrace()
			ImageFormats.decode(bytes)
		}
	}
}