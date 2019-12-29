package com.soywiz.korim.vector

import com.soywiz.korim.color.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class ShapeBuilderTest {
    @Test
    fun test() {
        val shape = buildShape {
            lineWidth = 8.0
            fillStroke(Context2d.Color(Colors.RED), Context2d.Color(Colors.BLUE)) {
                rect(0, 0, 64, 64)
            }
        }
        println(shape.toSvg())
    }
}
