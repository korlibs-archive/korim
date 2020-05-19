package com.soywiz.korim.vector

import com.soywiz.korim.format.*
import com.soywiz.korim.vector.format.*
import com.soywiz.korim.vector.format.SVG
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class SvgJvmTest {
    @Test
    fun test() = suspendTest {
        val svg = SVG(resourcesVfs["tiger.svg"].readString())
        //svg.renderToImage(512, 512).showImageAndWait()
        //svg.render().showImageAndWait()
        //svg.render(native = false).showImageAndWait()
    }

    @Test
    fun testTokenizePath() {
        val tokens = SVG.tokenizePath("m-122.3,84.285s0.1,1.894-0.73,1.875c-0.82-0.019-17.27-48.094-37.8-45.851,0,0,17.78-7.353,38.53,43.976z")
        //println(tokens)
    }
}
