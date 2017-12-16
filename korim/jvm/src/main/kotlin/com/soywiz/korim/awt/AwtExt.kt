package com.soywiz.korim.awt

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.coroutine.korioSuspendCoroutine
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel

fun Bitmap32.toAwt(out: BufferedImage = BufferedImage(width, height, if (this.premult) BufferedImage.TYPE_INT_ARGB_PRE else BufferedImage.TYPE_INT_ARGB)): BufferedImage {
	transferTo(out)
	return out
}

fun Bitmap.toAwt(out: BufferedImage = BufferedImage(width, height, if (this.premult) BufferedImage.TYPE_INT_ARGB_PRE else BufferedImage.TYPE_INT_ARGB)): BufferedImage = this.toBMP32().toAwt(out)

suspend fun awtShowImageAndWait(image: Bitmap): Unit = awtShowImageAndWait(image.toBMP32().toAwt())

suspend fun awtShowImageAndWait(image: BufferedImage): Unit = korioSuspendCoroutine { c ->
	awtShowImage(image).addWindowListener(object : WindowAdapter() {
		override fun windowClosing(e: WindowEvent) {
			c.resume(Unit)
		}
	})
}

fun awtShowImage(image: BufferedImage): JFrame {
	//println("Showing: $image")
	val frame = object : JFrame("Image (${image.width}x${image.height})") {

	}
	val label = JLabel()
	label.icon = ImageIcon(image)
	label.setSize(image.width, image.height)
	frame.add(label, BorderLayout.CENTER)
	//frame.setSize(bitmap.width, bitmap.height)
	frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
	frame.pack()
	frame.setLocationRelativeTo(null)
	frame.isVisible = true
	return frame
}

fun awtShowImage(bitmap: Bitmap) = awtShowImage(bitmap.toBMP32().toAwt())

fun awtConvertImage(image: BufferedImage): BufferedImage {
	val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB_PRE)
	out.graphics.drawImage(image, 0, 0, null)
	return out
}

fun awtConvertImageIfRequired(image: BufferedImage): BufferedImage = if ((image.type == BufferedImage.TYPE_INT_ARGB_PRE) || (image.type == BufferedImage.TYPE_INT_ARGB)) image else awtConvertImage(image)

fun Bitmap32.transferTo(out: BufferedImage): BufferedImage {
	val ints = (out.raster.dataBuffer as DataBufferInt).data
	System.arraycopy(this.data, 0, ints, 0, this.width * this.height)
	for (n in 0 until area) ints[n] = RGBA.rgbaToBgra(ints[n])
	out.flush()
	return out
}

val BufferedImage.premultiplied: Boolean get() = (this.type == BufferedImage.TYPE_INT_ARGB_PRE)

fun BufferedImage.toBMP32(): Bitmap32 {
	//println("Convert BufferedImage into BMP32!")
	val image = awtConvertImageIfRequired(this)
	val ints = (image.raster.dataBuffer as DataBufferInt).data
	val area = image.width * image.height
	for (n in 0 until area) ints[n] = RGBA.rgbaToBgra(ints[n])
	return Bitmap32(image.width, image.height, ints, premultiplied)
}

fun ImageIOReadFormat(s: InputStream, type: Int = AWT_INTERNAL_IMAGE_TYPE): BufferedImage {
	return ImageIO.read(s).clone(type = type)
	//return ImageIO.createImageInputStream(s).use { i ->
	//	// Get the reader
	//	val readers = ImageIO.getImageReaders(i)
//
	//	if (!readers.hasNext()) {
	//		throw IllegalArgumentException("No reader for: " + s) // Or simply return null
	//	}
//
	//	val reader = readers.next()
//
	//	try {
	//		// Set input
	//		reader.input = i
//
	//		// Configure the param to use the destination type you want
	//		val param = reader.defaultReadParam
	//		//param.destinationType = ImageTypeSpecifier.createFromBufferedImageType(type)
//
	//		// Finally read the image, using settings from param
	//		reader.read(0, param).clone(type = type)
	//	} finally {
	//		// Dispose reader in finally block to avoid memory leaks
	//		reader.dispose()
	//	}
	//}
}

fun awtReadImage(data: ByteArray): BufferedImage = ImageIOReadFormat(ByteArrayInputStream(data))
suspend fun awtReadImageInWorker(data: ByteArray): BufferedImage = executeInWorker { ImageIOReadFormat(ByteArrayInputStream(data)) }
suspend fun awtReadImageInWorker(file: File): BufferedImage = executeInWorker { FileInputStream(file).use { ImageIOReadFormat(it) } }

//var image = ImageIO.read(File("/Users/al/some-picture.jpg"))
