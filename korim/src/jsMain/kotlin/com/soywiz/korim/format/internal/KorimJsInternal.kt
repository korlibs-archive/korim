package com.soywiz.korim.format.internal

import com.soywiz.korio.lang.*

private external val require: (name: String) -> dynamic
private val myJsRequire = require
internal fun jsRequire(name: String) = myJsRequire(name)

val nodeJsCanvas: dynamic? by lazy { try { jsRequire("canvas") } catch (e: Throwable) {
    e.printStackTrace()
    null
} }
