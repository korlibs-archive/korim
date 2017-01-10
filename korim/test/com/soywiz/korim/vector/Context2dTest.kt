package com.soywiz.korim.vector

import com.soywiz.korim.awt.awtShowImageAndWait
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.sync
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Assert
import org.junit.Test

class Context2dTest {
	init {
		//System.setProperty("java.awt.headless", "true");
	}

	@Test
	fun name(): Unit = sync(EventLoopTest()) {
		val img = NativeImage(256, 256)
		val ctx = img.getContext2d()
		ctx.apply {
			keep {
				keep {
					scale(2.0, 2.0)
					rect(50.0, 20.0, 70.0, 70.0)
					clip()

					beginPath()
					moveTo(20, 20);               // Create a starting point
					lineTo(100, 20);              // Create a horizontal line
					//ctx.arcTo(150, 20, 150, 70, 50);  // Create an arc
					lineTo(150, 120);             // Continue with vertical line
					fill();                     // Draw it
				}

				fillStyle = Context2d.Color(Colors.GREEN)
				fillRect(0.0, 0.0, 50.0, 50.0)

				beginPath()
				fillStyle = Context2d.Color(Colors.GREEN)
				lineWidth = 10.0
				lineCap = Context2d.LineCap.ROUND
				moveTo(100.0, 100.0)
				lineTo(120, 120)
				rect(20.0, 20.0, 100.0, 100.0)
				stroke()
			}
		}
		Assert.assertTrue(
			Bitmap32.matches(
				ResourcesVfs["c2dreference.png"].readBitmap(),
				img.toBmp32()
			)
		)

		//LocalVfs("/tmp/file.png").writeBitmap(img.toBmp32())
		//awtShowImageAndWait(img)
	}

	@Test
	fun name2(): Unit = sync(EventLoopTest()) {
		val img = NativeImage(256, 256)
		val ctx = img.getContext2d()
		ctx.fillStyle = Context2d.Color(Colors.RED)
		ctx.lineWidth = 5.0

		/*
		ctx.beginPath()
		ctx.moveTo(128, 128)
		ctx.lineTo(500, 128)
		ctx.lineTo(500, 200)
		ctx.closePath()
		ctx.clip()

		ctx.beginPath()
		ctx.circle(128.0, 128.0, 64.0)
		ctx.circle(128.0, 128.0, 32.0)
		*/

		ctx.beginPath();
		ctx.moveTo(20, 20);               // Create a starting point
		ctx.lineTo(100, 20);              // Create a horizontal line
		ctx.arcTo(150, 20, 150, 70, 50);  // Create an arc
		ctx.lineTo(150, 120);             // Continue with vertical line
		ctx.stroke();                     // Draw it

		//ctx.arc(128.0, 128.0, 32.0, 0.0, Math.PI / 4.0)
		//ctx.arc(128.0, 128.0, 32.0, 0.0, (Math.PI * 2.0) * 0.9)
		//ctx.stroke()
		//ctx.fill()
		awtShowImageAndWait(img)
	}
}