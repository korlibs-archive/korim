package com.soywiz.korim.format

import com.soywiz.korim.bitmap.NativeImage

abstract class NativeImageFormatProvider {
	abstract suspend fun decode(data: ByteArray): NativeImage
}