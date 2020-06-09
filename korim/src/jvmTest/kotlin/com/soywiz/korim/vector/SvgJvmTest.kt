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

    @Test
    fun testSvg2() = suspendTest {
        val svgString = """
            <svg version="1.1" xmlns="http://www.w3.org/2000/svg" width="230" height="1024" viewBox="0 0 230 1024">
            <title></title>
            <g id="icomoon-ignore">
            </g>
            <path d="M161.118 242.688q0 18.432-13.312 31.232t-30.72 12.8q-19.456 0-33.792-12.288t-14.336-30.72q-1.024-20.48 11.776-34.816t31.232-14.336q20.48 0 34.816 13.824t14.336 34.304zM76.126 829.44v-450.56h69.632v450.56h-69.632z"></path>
            </svg>
        """.trimIndent()

        val image = SVG(svgString).render()
        //image.showImageAndWait()
    }
}
