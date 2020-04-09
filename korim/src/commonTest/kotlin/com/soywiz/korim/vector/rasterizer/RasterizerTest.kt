package com.soywiz.korim.vector.rasterizer

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.util.niceStr
import com.soywiz.korma.geom.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

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

}
