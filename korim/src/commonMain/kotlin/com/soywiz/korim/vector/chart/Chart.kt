package com.soywiz.korim.vector.chart

import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import kotlin.math.*

abstract class Chart() : Context2d.Drawable {
	abstract fun Context2d.renderChart()
}

