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

    // @TODO: Optimize this. Right now it is O(n^2)
    fun setToIntersect(a: SegmentSet, b: SegmentSet) {
        clear()

        a.fastForEach { x1, x2 ->
            b.fastForEach { y1, y2 ->
                if (intersects(x1, x2, y1, y2)) {
                    add(kotlin.math.max(x1, y1), kotlin.math.min(x2, y2))
                }
            }
        }
    }

    private fun intersects(x1: Int, x2: Int, y1: Int, y2: Int): Boolean = x2 >= y1 && y2 >= x1
}
