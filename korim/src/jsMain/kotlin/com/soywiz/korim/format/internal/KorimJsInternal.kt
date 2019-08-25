package com.soywiz.korim.format.internal

private external val require: (name: String) -> dynamic
private val myJsRequire = require
internal fun jsRequire(name: String) = myJsRequire(name)
