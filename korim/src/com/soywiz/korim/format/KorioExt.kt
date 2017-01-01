package com.soywiz.korim.format

import com.jtransc.annotation.JTranscMethodBody
import com.soywiz.korim.awt.awtReadImage
import com.soywiz.korim.awt.toBMP32
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.vfs.VfsFile
import kotlin.coroutines.CoroutineIntrinsics

@JTranscMethodBody(target = "js", value = """
	return {% SMETHOD com.soywiz.korim.format.BrowserImage:gen %}(p0, p1);
""")
private suspend fun gen(bytes: ByteArray): Bitmap = asyncFun {
	AwtImage.gen(bytes)
}

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
	@JTranscMethodBody(target = "js", value = """
        var bytes = p0, continuation = p1;

		var blob = new Blob([bytes.data], {type: 'image/png'});
		var blobURL = URL.createObjectURL(blob);

		var img = new Image();
		img.onload = function() {
			var canvas = document.createElement('canvas');
			canvas.width = img.width;
			canvas.height = img.height;
			var ctx = canvas.getContext('2d');
			ctx.drawImage(img, 0, 0);
			URL.revokeObjectURL(blobURL);

			//console.log('decoded image:', canvas);
			continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](canvas);
		};
		img.onerror = function() {
			//console.log('error decoding image:', img);
			continuation['{% METHOD kotlin.coroutines.Continuation:resumeWithException %}'](N.createRuntimeException('error loading image'));
		};
		//console.log(blobURL);
		img.src = blobURL;

		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external suspend fun loadjsimg(bytes: ByteArray): Any?

	@JTranscMethodBody(target = "js", value = """return p0 ? p0.width : -1;""")
	external fun imgWidth(img: Any?): Int

	@JTranscMethodBody(target = "js", value = """return p0 ? p0.height : -1;""")
	external fun imgHeight(img: Any?): Int

	@JTranscMethodBody(target = "js", value = """
		var canvas = p0, out = p1;
		var width = canvas.width, height = canvas.height, len = width * height;
		var ctx = canvas.getContext('2d')
		var data = ctx.getImageData(0, 0, width, height);
		var ddata = data.data;
		var m = 0;
		for (var n = 0; n < len; n++) {
			var r = ddata[m++];
			var g = ddata[m++];
			var b = ddata[m++];
			var a = ddata[m++];
			out.data[n] = (r << 0) | (g << 8) | (b << 16) | (a << 24);
		}
		//console.log(out);
	""")
	external fun imgData(canvas: Any?, out: IntArray): Unit

	@JvmStatic suspend fun gen(bytes: ByteArray): Bitmap = asyncFun {
		val img = BrowserImage.loadjsimg(bytes)
		val width = BrowserImage.imgWidth(img)
		val height = BrowserImage.imgHeight(img)
		val data = kotlin.IntArray(width * height)
		BrowserImage.imgData(img, data)
		Bitmap32(width, height, data)
	}

	@Suppress("unused")
	private fun getSuspended() = CoroutineIntrinsics.SUSPENDED
}

object AwtImage {
	@JvmStatic suspend fun gen(bytes: ByteArray): Bitmap = asyncFun {
		//println("AwtImage.gen!")
		awtReadImage(bytes).toBMP32()
	}
}