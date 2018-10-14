package com.soywiz.korim.bitmap

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.*

typealias Tex = BmpSlice

interface BmpSlice : Extra {
	val name: String
	var parent: Any?
	val bmp: Bitmap
	val tl_x: Float
	val tl_y: Float
	val tr_x: Float
	val tr_y: Float
	val bl_x: Float
	val bl_y: Float
	val br_x: Float
	val br_y: Float
	val left: Int
	val top: Int
	val width: Int
	val height: Int
	val rotated: Boolean
	val rotatedAngle: Int
}

class BitmapSlice<out T : Bitmap>(override val bmp: T, val bounds: RectangleInt, override val name: String = "unknown", rotated: Boolean = false) : BmpSlice, Extra by Extra.Mixin() {
	val premult get() = bmp.premult
	override var parent: Any? = null

	override val left get() = bounds.left
	override val top get() = bounds.top
	val right get() = bounds.right
	val bottom get() = bounds.bottom
	override val width get() = bounds.width
	override val height get() = bounds.height

	private val tl = Point(left.toFloat() / bmp.width.toFloat(), top.toFloat() / bmp.height.toFloat())
	private val br = Point(right.toFloat() / bmp.width.toFloat(), bottom.toFloat() / bmp.height.toFloat())
	private val tr = Point(br.x, tl.y)
	private val bl = Point(tl.x, br.y)

	private val points = arrayOf(tl, tr, br, bl)
	private val offset = if (rotated) 1 else 0

	private val p0 = points.getCyclic(offset + 0)
	private val p1 = points.getCyclic(offset + 1)
	private val p2 = points.getCyclic(offset + 2)
	private val p3 = points.getCyclic(offset + 3)

	override val tl_x = p0.x.toFloat()
	override val tl_y = p0.y.toFloat()

	override val tr_x = p1.x.toFloat()
	override val tr_y = p1.y.toFloat()

	override val br_x = p2.x.toFloat()
	override val br_y = p2.y.toFloat()

	override val bl_x = p3.x.toFloat()
	override val bl_y = p3.y.toFloat()

	fun extract(): T = bmp.extract(bounds.x, bounds.y, bounds.width, bounds.height)

	fun sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int): BitmapSlice<T> =
		BitmapSlice(
			bmp, RectangleInt.fromBounds(
				(left + bounds.left).clampX(),
				(top + bounds.top).clampY(),
				(right + bounds.left).clampX(),
				(bottom + bounds.top).clampY()
			)
		)

	fun sliceWithSize(x: Int, y: Int, width: Int, height: Int): BitmapSlice<T> =
		sliceWithBounds(x, y, x + width, y + height)

	fun slice(rect: RectangleInt): BitmapSlice<T> = sliceWithBounds(rect.left, rect.top, rect.right, rect.bottom)
	fun slice(rect: Rectangle): BitmapSlice<T> = slice(rect.toInt())

	private fun Int.clampX() = this.clamp(bounds.left, bounds.right)
	private fun Int.clampY() = this.clamp(bounds.top, bounds.bottom)

	override val rotated: Boolean = false
	override val rotatedAngle: Int = 0

	override fun toString(): String = "BitmapSlice($name:${bounds.size})"
}

// http://pixijs.download/dev/docs/PIXI.Texture.html#Texture
fun BitmapSliceCompat(
	bmp: Bitmap,
	frame: Rectangle,
	orig: Rectangle,
	trim: Rectangle,
	rotated: Boolean,
	name: String = "unknown"
) = BitmapSlice(bmp, frame.toInt(), name = name, rotated = rotated)

fun <T : Bitmap> T.slice(bounds: RectangleInt = RectangleInt(0, 0, width, height), name: String = "unknown"): BitmapSlice<T> = BitmapSlice<T>(this, bounds, name)
fun <T : Bitmap> T.sliceWithBounds(left: Int, top: Int, right: Int, bottom: Int): BitmapSlice<T> =
	BitmapSlice<T>(this, RectangleInt(left, top, right - left, bottom - top))

fun <T : Bitmap> T.sliceWithSize(x: Int, y: Int, width: Int, height: Int): BitmapSlice<T> =
	BitmapSlice<T>(this, RectangleInt(x, y, width, height))

