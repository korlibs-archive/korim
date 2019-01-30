package com.soywiz.korim.format.ios

import platform.CoreGraphics.*

actual fun Double.toCgFloat(): CGFloat = this.toFloat()
