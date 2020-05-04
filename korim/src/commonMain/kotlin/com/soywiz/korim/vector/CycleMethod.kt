package com.soywiz.korim.vector

import com.soywiz.kmem.*

enum class CycleMethod { NO_CYCLE, REFLECT, REPEAT }

fun CycleMethod.apply(ratio: Double): Double = when (this) {
    CycleMethod.NO_CYCLE -> ratio.clamp01()
    CycleMethod.REPEAT -> ratio % 1
    CycleMethod.REFLECT -> {
        val part = ratio % 2
        if (part > 1.0) 2.0 - part else part
    }
}
