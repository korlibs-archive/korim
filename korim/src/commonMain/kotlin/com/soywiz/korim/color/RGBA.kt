package com.soywiz.korim.color

import com.soywiz.kds.GenericListIterator
import com.soywiz.kds.GenericSubList
import com.soywiz.kmem.*
import com.soywiz.korim.internal.clamp0_255
import com.soywiz.korim.internal.clamp255
import com.soywiz.korio.lang.*
import com.soywiz.korma.interpolation.*

inline class RGBA(val rgba: Int) : Comparable<RGBA> {
	val r: Int get() = (rgba ushr 0) and 0xFF
	val g: Int get() = (rgba ushr 8) and 0xFF
	val b: Int get() = (rgba ushr 16) and 0xFF
	val a: Int get() = (rgba ushr 24) and 0xFF

	val rf: Float get() = r.toFloat() / 255f
	val gf: Float get() = g.toFloat() / 255f
	val bf: Float get() = b.toFloat() / 255f
	val af: Float get() = a.toFloat() / 255f

	val rd: Double get() = r.toDouble() / 255.0
	val gd: Double get() = g.toDouble() / 255.0
	val bd: Double get() = b.toDouble() / 255.0
	val ad: Double get() = a.toDouble() / 255.0

	val rgb: Int get() = rgba and 0xFFFFFF

	fun withR(v: Int) = RGBA((rgba and (0xFF shl 0).inv()) or ((v and 0xFF) shl 0))
	fun withG(v: Int) = RGBA((rgba and (0xFF shl 8).inv()) or ((v and 0xFF) shl 8))
	fun withB(v: Int) = RGBA((rgba and (0xFF shl 16).inv()) or ((v and 0xFF) shl 16))
	fun withA(v: Int) = RGBA((rgba and (0xFF shl 24).inv()) or ((v and 0xFF) shl 16))
	fun withRGB(rgb: Int) = RGBA(rgb, a)

	fun toInt(): Int = rgba

	val hexString: String get() ="#%02x%02x%02x%02x".format(r, g, b, a)
	val htmlColor: String get() = "rgba($r, $g, $b, $af)"
	val htmlStringSimple: String get() = "#%02x%02x%02x".format(r, g, b)

	override fun toString(): String = hexString

	operator fun plus(other: RGBA): RGBA = RGBA(this.r + other.r, this.g + other.g, this.b + other.b, this.a + other.a)
	operator fun minus(other: RGBA): RGBA = RGBA(this.r - other.r, this.g - other.g, this.b - other.b, this.a - other.a)

    override operator fun compareTo(other: RGBA): Int = this.rgba.compareTo(other.rgba)

	companion object : ColorFormat32() {
        fun unchecked(r: Int, g: Int, b: Int, a: Int): RGBA = RGBA(((r and 0xFF) shl 0) or ((g and 0xFF) shl 8) or ((b and 0xFF) shl 16) or ((a and 0xFF) shl 24))
		operator fun invoke(r: Int, g: Int, b: Int, a: Int): RGBA = unchecked(r.clamp0_255(), g.clamp0_255(), b.clamp0_255(), a.clamp0_255())
        operator fun invoke(r: Int, g: Int, b: Int): RGBA = unchecked(r.clamp0_255(), g.clamp0_255(), b.clamp0_255(), 0xFF)
		operator fun invoke(rgb: Int, a: Int): RGBA = RGBA((rgb and 0xFFFFFF) or (a shl 24))
		override fun getR(v: Int): Int = RGBA(v).r
		override fun getG(v: Int): Int = RGBA(v).g
		override fun getB(v: Int): Int = RGBA(v).b
		override fun getA(v: Int): Int = RGBA(v).a
		override fun pack(r: Int, g: Int, b: Int, a: Int): Int = RGBA(r, g, b, a).rgba

		fun premultiply(v: RGBA): RGBA = premultiplyFast(v)
		
		fun premultiplyAccurate(v: Int): Int {
			val a1 = RGBA(v).a
			val af = a1.toFloat() / 255f
			return RGBA.unchecked((RGBA(v).r * af).toInt(), (RGBA(v).g * af).toInt(), (RGBA(v).b * af).toInt(), a1).rgba
		}

		fun premultiplyFast(v: RGBA): RGBA {
            val A = v.a + 1
            val RB = (((v.rgba and 0x00FF00FF) * A) ushr 8) and 0x00FF00FF
            val G = (((v.rgba and 0x0000FF00) * A) ushr 8) and 0x0000FF00
            return RGBA((v.rgba and 0x00FFFFFF.inv()) or RB or G)
        }

		fun mutliplyByAlpha(v: Int, alpha: Double): Int = com.soywiz.korim.color.RGBA.pack(RGBA(v).r, RGBA(v).g, RGBA(v).b, (RGBA(v).a * alpha).toInt())
		fun depremultiply(v: RGBA): RGBA = depremultiplyFast(v)

		fun depremultiplyAccurate(v: RGBA): RGBA {
			val alpha = v.ad
            return when (alpha) {
                0.0 -> Colors.TRANSPARENT_WHITE
                else -> {
                    val ialpha = 1.0 / alpha
                    RGBA((v.r * ialpha).toInt(), (v.g * ialpha).toInt(), (v.b * ialpha).toInt(), v.a)
                }
            }
		}

		fun depremultiplyFast(v: RGBA): RGBA {
            val A = v.a
            val alpha = A.toDouble() / 255.0
            if (alpha == 0.0) return Colors.TRANSPARENT_BLACK
            val ialpha = 1.0 / alpha
            val R = (v.r * ialpha).toInt().clamp255()
            val G = (v.g * ialpha).toInt().clamp255()
            val B = (v.b * ialpha).toInt().clamp255()
            return RGBA.unchecked(R, G, B, A)
        }

		fun depremultiplyFast(data: RgbaArray, start: Int = 0, end: Int = data.size): RgbaArray = data.apply {
			for (n in start until end) data[n] = depremultiplyFast(data[n])
		}

		fun premultiplyFast(data: RgbaArray, start: Int = 0, end: Int = data.size): RgbaArray = data.apply {
			val array = data.ints
			for (n in start until end) array[n] = premultiplyFast(RGBA(array[n])).rgba
		}

		fun depremultiplyFaster(v: Int): Int {
			val A = (v ushr 24)
			val A1 = A + 1
			val R = ((((v ushr 0) and 0xFF) shl 8) / A1) and 0xFF
			val G = ((((v ushr 8) and 0xFF) shl 8) / A1) and 0xFF
			val B = ((((v ushr 16) and 0xFF) shl 8) / A1) and 0xFF
			return RGBA(R, G, B, A).rgba
		}

		fun depremultiplyFastest(v: Int): Int {
			val A = (v ushr 24) + 1
			val R = (((v and 0x0000FF) shl 8) / A) and 0x0000F0
			val G = (((v and 0x00FF00) shl 8) / A) and 0x00FF00
			val B = (((v and 0xFF0000) shl 8) / A) and 0xFF0000
			return (v and 0x00FFFFFF.inv()) or B or G or R
		}

		fun packfFast(r: Float, g: Float, b: Float, a: Float): Int = ((r * 0xFF).toInt() shl 0) or ((g * 0xFF).toInt() shl 8) or ((b * 0xFF).toInt() shl 16) or ((a * 0xFF).toInt() shl 24)
		fun packRGB_A(rgb: Int, a: Int): Int = (rgb and 0xFFFFFF) or (a shl 24)
		fun blendComponent(c1: Int, c2: Int, factor: Double): Int = (c1 * (1.0 - factor) + c2 * factor).toInt() and 0xFF
		fun blendRGB(c1: Int, c2: Int, factor256: Int): Int = (256 - factor256).let { f1 -> ((((((c1 and 0xFF00FF) * f1) + ((c2 and 0xFF00FF) * factor256)) and 0xFF00FF00.toInt()) or ((((c1 and 0x00FF00) * f1) + ((c2 and 0x00FF00) * factor256)) and 0x00FF0000))) ushr 8 }
        fun blendRGB(c1: RGBA, c2: RGBA, factor256: Int): RGBA = RGBA(blendRGB(c1.rgba, c2.rgba, factor256))
		fun blendRGB(c1: Int, c2: Int, factor: Double): Int = blendRGB(c1, c2, (factor * 256).toInt())
		fun blendRGBAInt(c1: Int, c2: Int, factor: Double): Int = blendRGBA(RGBA(c1), RGBA(c2), factor).rgba
		fun blendRGBA(c1: RGBA, c2: RGBA, factor: Double): RGBA {
			val RGB = blendRGB(c1.rgba and 0xFFFFFF, c2.rgba and 0xFFFFFF, (factor * 256).toInt())
			val A = blendComponent(c1.a, c2.a, factor)
			return RGBA(packRGB_A(RGB, A))
		}

		
		fun rgbaToBgra(v: Int) =
			((v shl 16) and 0x00FF0000) or ((v shr 16) and 0x000000FF) or (v and 0xFF00FF00.toInt())

		
		private fun d2i(v: Double): Int = ((v.toFloat()).clamp01() * 255).toInt()
		private fun f2i(v: Float): Int = ((v).clamp01() * 255).toInt()
		fun packf(r: Double, g: Double, b: Double, a: Double): RGBA = RGBA.unchecked(d2i(r), d2i(g), d2i(b), d2i(a))
		fun packf(r: Float, g: Float, b: Float, a: Float): Int = RGBA(f2i(r), f2i(g), f2i(b), f2i(a)).rgba
		fun packf(rgb: Int, a: Float): Int = packRGB_A(rgb, f2i(a))
		fun mix(dst: RGBA, src: RGBA): RGBA {
            val srcA = src.a
            return when (srcA) {
                0x000 -> dst
                0xFF -> src
                else -> RGBA(blendRGB(dst.rgb, src.rgb, srcA + 1), dst.a + srcA)
            }
        }

		fun interpolate(src: RGBA, dst: RGBA, ratio: Double): RGBA = RGBA(
            ratio.interpolate(src.r, dst.r),
            ratio.interpolate(src.g, dst.g),
            ratio.interpolate(src.b, dst.b),
            ratio.interpolate(src.a, dst.a)
        )

		fun multiply(c1: RGBA, c2: RGBA): RGBA = RGBA(
            ((c1.r * c2.r) / 0xFF).clamp0_255(),
            ((c1.g * c2.g) / 0xFF).clamp0_255(),
            ((c1.b * c2.b) / 0xFF).clamp0_255(),
            ((c1.a * c2.a) / 0xFF).clamp0_255()
        )

		fun blendRGBAFastAlreadyPremultiplied_05(c1: Int, c2: Int): Int {
			val RB = (((c1 and 0xFF00FF) + (c2 and 0xFF00FF)) ushr 1) and 0xFF00FF
			val G = (((c1 and 0x00FF00) + (c2 and 0x00FF00)) ushr 1) and 0x00FF00
			val A = (((c1 ushr 24) + (c2 ushr 24)) ushr 1) and 0xFF
			return (A shl 24) or RB or G
		}

		fun blendRGBAFastAlreadyPremultiplied_05(c1: Int, c2: Int, c3: Int, c4: Int): Int {
			val RB = (((c1 and 0xFF00FF) + (c2 and 0xFF00FF) + (c3 and 0xFF00FF) + (c4 and 0xFF00FF)) ushr 2) and 0xFF00FF
			val G = (((c1 and 0x00FF00) + (c2 and 0x00FF00) + (c3 and 0x00FF00) + (c4 and 0x00FF00)) ushr 2) and 0x00FF00
			val A = (((c1 ushr 24) + (c2 ushr 24) + (c3 ushr 24) + (c4 ushr 24)) ushr 2) and 0xFF
			return (A shl 24) or RB or G
		}
	}
}

inline class RgbaArray(val ints: IntArray) : List<RGBA> {
    companion object {
        operator fun invoke(colors: Array<RGBA>): RgbaArray = RgbaArray(colors.map { it.rgba }.toIntArray())
        operator fun invoke(size: Int): RgbaArray = RgbaArray(IntArray(size))
        operator fun invoke(size: Int, callback: (index: Int) -> RGBA): RgbaArray = RgbaArray(IntArray(size)).apply { for (n in 0 until size) this[n] = callback(n) }
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<RGBA> = GenericSubList(this, fromIndex, toIndex)
	override fun contains(element: RGBA): Boolean = ints.contains(element.rgba)
	override fun containsAll(elements: Collection<RGBA>): Boolean = elements.all { contains(it) }
	override fun indexOf(element: RGBA): Int = ints.indexOf(element.rgba)
	override fun lastIndexOf(element: RGBA): Int = ints.lastIndexOf(element.rgba)
	override fun isEmpty(): Boolean = ints.isEmpty()
	override fun iterator(): Iterator<RGBA> = listIterator(0)
	override fun listIterator(): ListIterator<RGBA> = listIterator(0)
	override fun listIterator(index: Int): ListIterator<RGBA> = GenericListIterator(this, index)

	override val size get() = ints.size
	override operator fun get(index: Int): RGBA = RGBA(ints[index])
	operator fun set(index: Int, color: RGBA) = run { ints[index] = color.rgba }
	fun fill(value: RGBA, start: Int = 0, end: Int = this.size): Unit = ints.fill(value.rgba, start, end)

	override fun toString(): String = "RgbaArray($size)"
}

fun RGBA.mix(other: RGBA, ratio: Double) = RGBA.blendRGBA(this, other, ratio)

fun List<RGBA>.toRgbaArray(): RgbaArray = RgbaArray(IntArray(this.size) { this@toRgbaArray[it].rgba })

fun arraycopy(src: RgbaArray, srcPos: Int, dst: RgbaArray, dstPos: Int, size: Int): Unit = arraycopy(src.ints, srcPos, dst.ints, dstPos, size)

fun RGBA.Companion.depremultiplyFaster(v: RGBA): RGBA = RGBA(RGBA.depremultiplyFaster(v.toInt()))
fun RGBA.Companion.depremultiplyFastest(v: RGBA): RGBA = RGBA(RGBA.depremultiplyFastest(v.toInt()))

fun Array<RGBA>.toRgbaArray() = RgbaArray(this.size) { this@toRgbaArray[it] }
