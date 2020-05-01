package com.soywiz.korim.vector.rasterizer

import com.soywiz.kds.*

class SegmentSet {
    @PublishedApi
    internal val min = IntArrayList(16)
    @PublishedApi
    internal val max = IntArrayList(16)
    val size get() = min.size

    fun clear() {
        min.clear()
        max.clear()
    }

    fun add(min: Int, max: Int) {
        check(min <= max)
        this.min.add(min)
        this.max.add(max)
    }

    inline fun fastForEach(block: (min: Int, max: Int) -> Unit) {
        for (n in 0 until size) {
            block(min.getAt(n), max.getAt(n))
        }
    }

    // @TODO: Optimize this
    fun setToIntersect(a: SegmentSet, b: SegmentSet) {
        clear()
        a.fastForEach { amin, amax ->
            b.fastForEach { bmin, bmax ->

            }
        }
    }
}
