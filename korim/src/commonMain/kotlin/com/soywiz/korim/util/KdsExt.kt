package com.soywiz.korim.util

// @TODO: Move to KDS
@PublishedApi
internal inline fun genericRemoveSortedDuplicates(size: Int, equals: (x: Int, y: Int) -> Boolean, copy: (src: Int, dst: Int) -> Unit, resize: (size: Int) -> Unit) {
    if (size < 2) return
    var pivot = 0
    var ref = 0
    while (true) {
        while (ref < size && equals(pivot, ref)) ref++
        if (ref >= size) break
        copy(ref, ++pivot)
    }
    resize(pivot + 1)
}

internal fun <T> ArrayList<T>.removeSortedDuplicates() = this.apply {
    genericRemoveSortedDuplicates(
        size = size,
        equals = { x, y -> this[x] == this[y] },
        copy = { src, dst -> this[dst] = this[src] },
        resize = { size -> while (this.size > size && size >= 0) this.removeAt(this.size - 1) }
    )
}
