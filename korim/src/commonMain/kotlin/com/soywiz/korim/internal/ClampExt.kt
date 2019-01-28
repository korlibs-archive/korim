package com.soywiz.korim.internal

import com.soywiz.kmem.clamp
import kotlin.math.min

internal fun Double.clampf1() = if (this > 1.0) 1.0 else this
internal fun Int.clamp0_255() = clamp(0, 255)
internal fun Int.clamp255() = if (this > 255) 255 else this
