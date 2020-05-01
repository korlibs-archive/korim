package com.soywiz.korim.vector.rasterizer

import com.soywiz.kds.*
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

typealias RasterizerCallback = (x0: Int, x1: Int, y: Int) -> Unit

class Rasterizer : RastScale() {
    var debug: Boolean = false
    private val tempRect = Rectangle()
    var quality: Int = 2

    data class Stats(
        var edgesChecked: Int = 0,
        var edgesEmitted: Int = 0,
        var yCount: Int = 0
    ) {
        fun reset() {
            edgesChecked = 0
            edgesEmitted = 0
            yCount = 0
        }

        fun chunk(edgesChecked: Int, edgesEmitted: Int, yCount: Int) {
            this.edgesChecked += edgesChecked
            this.edgesEmitted += edgesEmitted
            this.yCount += yCount
        }
    }

    var scale: Int = 1
    //val sscale get() = RAST_FIXED_SCALE * scale
    //val hscale get() = RAST_FIXED_SCALE_HALF * scale

    val path = PolygonScanline()
    val clip = PolygonScanline()

    private val fillSegmentSet = SegmentSet()
    private val clipSegmentSet = SegmentSet()
    private val finalSegmentSet = SegmentSet()

    fun rasterizeFill(bounds: Rectangle, quality: Int = this.quality, stats: Stats? = null, winding: Winding = Winding.NON_ZERO, callback: RasterizerCallback) {
        stats?.reset()
        val xmin = bounds.left.s
        val xmax = bounds.right.s
        path.boundsBuilder.getBounds(tempRect)
        val startY = max(bounds.top, tempRect.top).s
        val endY = min(bounds.bottom, tempRect.bottom).s
        val func: (x0: Int, x1: Int, y: Int) -> Unit = { a, b, y ->
            //println("CHUNK")
            if (a <= xmax && b >= xmin) {
                //println("  - EMIT")
                val a0 = a.coerceIn(xmin, xmax)
                val b0 = b.coerceIn(xmin, xmax)
                if (debug) {
                    println("RASTER($a0, $b0, $y)")
                }
                callback(a0, b0, y)
            } else {
                // Discarded
                //println("  - DISCARDED")
            }
        }

        yList.clear()
        val q = quality
        //val q = if (quality == 1) 1 else quality + 1
        val yCount = ((((endY - startY) / sscale).toInt()) + 1) * q
        for (n in 0 until yCount + q - 1) {
            val y = (startY + (n * sscale / scale) / q)
            yList.add(y)
        }
        //println("yList: ${yList.size}")

        run {
            var edgesEmitted = 0
            var edgesChecked = 0

            yList.fastForEach { y ->
                path.scanline(y, winding, fillSegmentSet)
                edgesChecked += path.edgesChecked

                fillSegmentSet.fastForEach { min, max ->
                    func(min, max, y)
                    edgesEmitted++
                }
            }
            stats?.chunk(edgesChecked, edgesEmitted, yCount)
        }
    }
    private val yList = IntArrayList(1024)

    var strokeWidth: Double = 1.0
}
