package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap

suspend fun Bitmap.showImageAndWaitExt() = showImageAndWait(this)
suspend fun ImageData.showImagesAndWaitExt() = showImagesAndWait(this)

suspend fun showImageAndWait(image: Bitmap): Unit {
	println("Showing... $image")
	nativeImageFormatProvider.display(image)
}

suspend fun showImagesAndWait(image: ImageData) {
	for (frame in image.frames) {
		showImageAndWait(frame.bitmap)
	}
}
