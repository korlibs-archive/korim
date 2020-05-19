package com.soywiz.korim.vector

import com.soywiz.korim.vector.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class SvgJvmTest {
    @Test
    fun test() = suspendTest {
        val svg = SVG(resourcesVfs["tiger.svg"].readString())
    }
}
