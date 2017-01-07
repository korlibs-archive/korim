package com.soywiz.korim.awt

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import java.awt.BorderLayout
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel

fun Bitmap32.toAwt(out: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)): BufferedImage {
	transferTo(out)
	return out
}

fun awtShowImage(image: BufferedImage): JFrame {
	println("Showing: $image")
	val frame = JFrame("Image (${image.width}x${image.height})")
	val label = JLabel()
	label.icon = ImageIcon(image)
	label.setSize(image.width, image.height)
	frame.add(label, BorderLayout.CENTER)
	//frame.setSize(bitmap.width, bitmap.height)
	frame.pack()
	frame.setLocationRelativeTo(null)
	frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
	frame.isVisible = true
	return frame
}

fun awtShowImage(bitmap: Bitmap) = awtShowImage(bitmap.toBMP32().toAwt())

fun awtConvertImage(image: BufferedImage): BufferedImage {
	val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
	out.graphics.drawImage(image, 0, 0, null)
	return out
}

fun awtConvertImageIfRequired(image: BufferedImage): BufferedImage = if (image.type == BufferedImage.TYPE_INT_ARGB) image else awtConvertImage(image)

fun Bitmap32.transferTo(out: BufferedImage): BufferedImage {
	val ints = (out.raster.dataBuffer as DataBufferInt).data
	System.arraycopy(this.data, 0, ints, 0, this.width * this.height)
	for (n in 0 until area) ints[n] = RGBA.rgbaToBgra(ints[n])
	out.flush()
	return out
}

fun BufferedImage.toBMP32(): Bitmap32 {
	//println("Convert BufferedImage into BMP32!")
	val image = awtConvertImageIfRequired(this)
	val ints = (image.raster.dataBuffer as DataBufferInt).data
	val area = image.width * image.height
	for (n in 0 until area) ints[n] = RGBA.rgbaToBgra(ints[n])
	return Bitmap32(image.width, image.height, ints)
}

fun awtReadImage(data: ByteArray): BufferedImage = ImageIO.read(ByteArrayInputStream(data))

//var image = ImageIO.read(File("/Users/al/some-picture.jpg"))
