package com.soywiz.korim.awt

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import java.awt.BorderLayout
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel

fun Bitmap32.toAwt(): BufferedImage {
	val out = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
	val ints = (out.raster.dataBuffer as DataBufferInt).data
	java.lang.System.arraycopy(this.data, 0, ints, 0, this.width * this.height)
	for (n in 0 until area) ints[n] = RGBA.rgbaToBgra(ints[n])
	out.flush()
	return out
}

fun showImage(image: BufferedImage): JFrame {
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

fun showImage(bitmap: Bitmap) = showImage(bitmap.toBMP32().toAwt())

fun convertImage(image: BufferedImage): BufferedImage {
	val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
	out.graphics.drawImage(image, 0, 0, null)
	return out
}

fun Bitmap32.transferTo(out: BufferedImage): BufferedImage {
	val ints = (out.raster.dataBuffer as DataBufferInt).data
	System.arraycopy(this.data, 0, ints, 0, this.width * this.height)
	out.flush()
	return out
}
