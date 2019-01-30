package com.soywiz.korim.format.ios

import platform.CoreGraphics.*

expect fun Double.toCgFloat(): CGFloat
inline fun Number.toCgFloat(): CGFloat = this.toDouble().toCgFloat()
