package com.soywiz.korim.vector.rasterizer

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class RasterizerTest {
    @Test
    fun test() = suspendTest {
        //Bitmap32(100, 100).context2d {
        //    fill(Colors.RED) {
        //        circle(50, 50, 40)
        //    }
        //}.writeTo("/tmp/1/b.png".uniVfs, PNG)

        val rast = Rasterizer()
        rast.quality = 1
        rast.reset()
        rast.add(0, 10)
        rast.add(2, 0)
        rast.add(10, 0)
        rast.add(10, 10)
        rast.close()
        val log = arrayListOf<String>()
        rast.rasterizeFill(Rectangle(0, 0, 10, 10)) { a, b, y ->
            log += "rast(${a.niceStr}, ${b.niceStr}, ${y.niceStr})"
            println(log.last())
        }
    }

    @Test
    @Ignore
    fun test2() = suspendTest {
        Bitmap32(100, 100).context2d {
            //debug = true
            fill(
                createLinearGradient(0, 0, 0, 100) {
                    add(0.0, Colors.BLUE)
                    add(1.0, Colors.GREEN)
                }
            ) {
                moveTo(0, 25)
                lineTo(100, 0)
                lineToV(100)
                lineToH(-100)
                close()
            }
        }.showImageAndWait()
        val shipSize = 24
        Bitmap32(shipSize, shipSize).context2d {
            stroke(Colors.RED, lineWidth = shipSize * 0.05, lineCap = LineCap.ROUND) {
                moveTo(shipSize * 0.5, 0)
                lineTo(shipSize, shipSize)
                lineTo(shipSize * 0.5, shipSize * 0.8)
                lineTo(0, shipSize)
                close()
            }
        }.showImageAndWait()
        Bitmap32(3, (shipSize * 0.3).toInt()).context2d {
            lineWidth = 1.0
            lineCap = LineCap.ROUND
            stroke(Colors.WHITE) {
                moveTo(width / 2, 0)
                lineToV(height)
            }
        }.showImageAndWait()
        //bulletBitmap.showImageAndWait()
    }
}
