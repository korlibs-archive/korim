package com.soywiz.korim.vector.rasterizer

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
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
        rast.reset()
        rast.add(0, 10)
        rast.add(2, 0)
        rast.add(10, 0)
        rast.add(10, 10)
        rast.close()
        val log = arrayListOf<String>()
        rast.rasterizeFill(Rectangle(0, 0, 10, 10)) { a, b, y, alpha ->
            log += "rast(${a.niceStr}, ${b.niceStr}, ${y.toDouble().niceStr}, ${alpha.niceStr})"
            println(log.last())
        }
        assertEquals(listOf(
            "rast(2, 10, 0, 1)",
            "rast(1.8, 10, 1, 1)",
            "rast(1.6, 10, 2, 1)",
            "rast(1.4, 10, 3, 1)",
            "rast(1.2, 10, 4, 1)",
            "rast(1, 10, 5, 1)",
            "rast(0.8, 10, 6, 1)",
            "rast(0.6, 10, 7, 1)",
            "rast(0.4, 10, 8, 1)",
            "rast(0.2, 10, 9, 1)"
        ).joinToString("\n"), log.joinToString("\n"))
    }

    @Test
    fun test2() {
        val shipSize = 24
        val shipBitmap = Bitmap32(shipSize, shipSize).context2d {
            stroke(Colors.WHITE, lineWidth = shipSize * 0.05, lineCap = Context2d.LineCap.ROUND) {
                moveTo(shipSize * 0.5, 0)
                lineTo(shipSize, shipSize)
                lineTo(shipSize * 0.5, shipSize * 0.8)
                lineTo(0, shipSize)
                close()
            }
        }
        val bulletBitmap = Bitmap32(3, (shipSize * 0.3).toInt()).context2d {
            lineWidth = 1.0
            lineCap = Context2d.LineCap.ROUND
            stroke(Colors.WHITE) {
                moveTo(width / 2, 0)
                lineToV(height)
            }
        }
    }
}
