package com.soywiz.korim.vector

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*

// References:
// - https://github.com/memononen/nanosvg/blob/master/src/nanosvgrast.h
// - https://www.geeksforgeeks.org/scan-line-polygon-filling-using-opengl-c/
// - https://hackernoon.com/computer-graphics-scan-line-polygon-fill-algorithm-3cb47283df6
// - https://nothings.org/gamedev/rasterize/
class Bitmap32Context2d(val bmp: Bitmap32, val antialiasing: Boolean) : Context2d.Renderer() {
	override val width: Int get() = bmp.width
	override val height: Int get() = bmp.height

	val colorFiller = ColorFiller()
	val gradientFiller = GradientFiller()
	val bitmapFiller = BitmapFiller(antialiasing)
	val noneFiller = NoneFiller()

	// Super slow
	override fun render(state: Context2d.State, fill: Boolean) {
		//println("RENDER")
		val fillStyle = state.fillStyle
		val filler = when (fillStyle) {
			is Context2d.None -> noneFiller.apply { this.set(fillStyle, state) }
			is Context2d.Color -> colorFiller.apply { this.set(fillStyle, state) }
			is Context2d.Gradient -> gradientFiller.apply { this.set(fillStyle, state) }
			is Context2d.BitmapPaint -> bitmapFiller.apply { this.set(fillStyle, state) }
			else -> TODO()
		}
		val points = state.path.getApproximatedPoints().map { it.transformed(state.transform) }
		val edges = arrayListOf<Edge>()
		for (n in 0 until points.size) {
			val a = points[n]
			val b = points[(n + 1) % points.size]
			val edge = if (a.y < b.y) Edge(a, b, +1) else Edge(b, a, -1)
			if (edge.isNotCoplanarX) {
				edges += edge
			}
		}
		val bounds = points.bounds()
		//println("bounds:$bounds")
		for (y in bounds.top.toInt()..bounds.bottom.toInt()) {
			if (y !in 0 until bmp.height) continue // Calculate right range instead of skipping

			// @TODO: Optimize
			val xx = edges.filter { it.containsY(y) }.map { Point2d(it.intersectX(y), y) }.sortedBy2 { it.x }
				.map { it.x.toInt() }
			for (n in 0 until xx.size - 1) {
				val a = xx[n + 0].clamp(0, bmp.width)
				val b = xx[n + 1].clamp(0, bmp.width)

				// @TODO: Use winding information?
				if (n % 2 == 0) {
					filler.fill(bmp.data, bmp.index(a, y), a, y, b - a)
				}
			}
			//println("y:$y -- $xx")
		}
		//bmp.fill(Colors.PINK)
	}

	fun VectorPath.getApproximatedPoints(): List<Point2d> {
		return this.toPaths2().flatMap { it }
	}

	data class Edge(val a: Point2d, val b: Point2d, val wind: Int) {
		val isCoplanarX = a.y == b.y
		val isNotCoplanarX get() = !isCoplanarX

		val isCoplanarY = a.x == b.x

		private val slope = (b.y - a.y) / (b.x - a.x)
		private val h = a.y - (a.x * slope)

		//init {
		//println("a=$a,b=$b :: h=$h,slope=$slope, coplanaer=")
		//}

		fun containsY(y: Int): Boolean = y >= a.y && y < b.y
		fun intersectX(y: Int): Double = if (isCoplanarY) a.x else ((y - h) / slope)
	}

	abstract class Filler<T : Context2d.Paint> {
		protected lateinit var fill: T
		protected lateinit var state: Context2d.State

		fun set(paint: T, state: Context2d.State) {
			this.fill = paint
			this.state = state
			updated()
		}

		open fun updated() {
		}

		abstract fun fill(data: RgbaArray, offset: Int, x: Int, y: Int, count: Int)
	}

	class NoneFiller : Filler<Context2d.None>() {
		override fun fill(data: RgbaArray, offset: Int, x: Int, y: Int, count: Int) {
		}
	}

	class ColorFiller : Filler<Context2d.Color>() {
		override fun fill(data: RgbaArray, offset: Int, x: Int, y: Int, count: Int) {
			val c = fill.color.rgba
			for (n in 0 until count) {
				data.array[offset + n] = c
			}
		}
	}

	class BitmapFiller(val antialiasing: Boolean) : Filler<Context2d.BitmapPaint>() {
		lateinit var stateTrans: Matrix2d
		lateinit var fillTrans: Matrix2d

		override fun updated() {
			stateTrans = state.transform.inverted()
			fillTrans = fill.transform.inverted()
		}

		override fun fill(data: RgbaArray, offset: Int, x: Int, y: Int, count: Int) {
			for (n in 0 until count) {
				// @TODO: Optimize. We can calculate start and end points and interpolate
				val bmpX = fillTrans.transformX(x + n, y)
				val bmpY = fillTrans.transformY(y + n, y)
				if (antialiasing) {
					data.array[offset + n] = fill.bitmap.get32SampledInt(bmpX, bmpY)
				} else {
					data.array[offset + n] = fill.bitmap.get32ClampedInt(bmpX.toInt(), bmpY.toInt())
				}
			}
		}
	}

	class GradientFiller : Filler<Context2d.Gradient>() {
		val NCOLORS = 256
		val colors = RgbaArray(NCOLORS)

		fun stopN(n: Int): Int = (fill.stops[n] * NCOLORS).toInt()

		lateinit var stateTrans: Matrix2d
		lateinit var fillTrans: Matrix2d

		override fun updated() {
			stateTrans = state.transform.inverted()
			fillTrans = fill.transform.inverted()
			for (n in 0 until stopN(0)) colors.array[n] = RGBAInt(fill.colors.first())
			for (n in 0 until fill.numberOfStops - 1) {
				val stop0 = stopN(n + 0)
				val stop1 = stopN(n + 1)
				val color0 = RGBAInt(fill.colors[n + 0])
				val color1 = RGBAInt(fill.colors[n + 1])
				for (s in stop0 until stop1) {
					val ratio = (s - stop0).toDouble() / (stop1 - stop0).toDouble()
					colors.array[s] = RGBA.interpolateInt(color0, color1, ratio)
				}
			}
			for (n in stopN(fill.numberOfStops - 1) until NCOLORS) colors.array[n] = fill.colors.last()
			//println(colors.map { RGBA.toHexString(it) })
		}

		override fun fill(data: RgbaArray, offset: Int, x: Int, y: Int, count: Int) {

			val p0 = Point2d(fill.x0, fill.y0)
			val p1 = Point2d(fill.x1, fill.y1)

			val mat = Matrix2d().apply {
				multiply(this, stateTrans)
				multiply(this, fillTrans)
				translate(-p0.x, -p0.y)
				scale(1.0 / (p0.distanceTo(p1)).clamp(1.0, 16000.0))
				rotate(-Angle.betweenRad(p0, p1))
			}

			for (n in 0 until count) {
				val ratio = mat.transformX((x + n).toDouble(), y.toDouble()).clamp01()
				data.array[offset + n] = colors.array[(ratio * (NCOLORS - 1)).toInt()]
			}
		}
	}
}