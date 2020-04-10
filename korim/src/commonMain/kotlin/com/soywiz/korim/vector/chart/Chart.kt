package com.soywiz.korim.vector.chart

import com.soywiz.korim.vector.Context2d

abstract class Chart() : Context2d.Drawable {
	abstract fun Context2d.renderChart()
}

