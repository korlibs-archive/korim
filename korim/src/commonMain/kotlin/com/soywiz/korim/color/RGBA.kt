package com.soywiz.korim.color

import com.soywiz.kds.GenericListIterator
import com.soywiz.kds.GenericSubList
import com.soywiz.kmem.*
import com.soywiz.korim.internal.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.interpolation.*

inline class RGBA(val value: Int) : Comparable<RGBA>, Interpolable<RGBA> {
    val r: Int get() = (value ushr 0) and 0xFF
	val g: Int get() = (value ushr 8) and 0xFF
	val b: Int get() = (value ushr 16) and 0xFF
	val a: Int get() = (value ushr 24) and 0xFF

	val rf: Float get() = r.toFloat() / 255f
	val gf: Float get() = g.toFloat() / 255f
	val bf: Float get() = b.toFloat() / 255f
	val af: Float get() = a.toFloat() / 255f

	val rd: Double get() = r.toDouble() / 255.0
	val gd: Double get() = g.toDouble() / 255.0
	val bd: Double get() = b.toDouble() / 255.0
	val ad: Double get() = a.toDouble() / 255.0

	val rgb: Int get() = value and 0xFFFFFF

	fun withR(v: Int) = RGBA((value and (0xFF shl 0).inv()) or ((v and 0xFF) shl 0))
	fun withG(v: Int) = RGBA((value and (0xFF shl 8).inv()) or ((v and 0xFF) shl 8))
	fun withB(v: Int) = RGBA((value and (0xFF shl 16).inv()) or ((v and 0xFF) shl 16))
	fun withA(v: Int) = RGBA((value and (0xFF shl 24).inv()) or ((v and 0xFF) shl 16))
	fun withRGB(rgb: Int) = RGBA(rgb, a)

	fun toInt(): Int = value

	val hexString: String get() ="#%02x%02x%02x%02x".format(r, g, b, a)
	val htmlColor: String get() = "rgba($r, $g, $b, $af)"
	val htmlStringSimple: String get() = "#%02x%02x%02x".format(r, g, b)

	override fun toString(): String = hexString

	operator fun plus(other: RGBA): RGBA = RGBA(this.r + other.r, this.g + other.g, this.b + other.b, this.a + other.a)
	operator fun minus(other: RGBA): RGBA = RGBA(this.r - other.r, this.g - other.g, this.b - other.b, this.a - other.a)

    override operator fun compareTo(other: RGBA): Int = this.value.compareTo(other.value)
    override fun interpolateWith(ratio: Double, other: RGBA): RGBA = RGBA.interpolate(this, other, ratio)

    val premultiplied: RGBAPremultiplied get() {
        val A = a + 1
        val RB = (((value and 0x00FF00FF) * A) ushr 8) and 0x00FF00FF
        val G = (((value and 0x0000FF00) * A) ushr 8) and 0x0000FF00
        return RGBAPremultiplied((value and 0x00FFFFFF.inv()) or RB or G)
    }

    infix fun mix(dst: RGBA): RGBA = RGBA.mix(this, dst)
    operator fun times(other: RGBA): RGBA = RGBA.multiply(this, other)

    companion object : ColorFormat32() {
        fun float(r: Float, g: Float, b: Float, a: Float): RGBA = unclamped(f2i(r), f2i(g), f2i(b), f2i(a))
        fun unclamped(r: Int, g: Int, b: Int, a: Int): RGBA = RGBA(((r and 0xFF) shl 0) or ((g and 0xFF) shl 8) or ((b and 0xFF) shl 16) or ((a and 0xFF) shl 24))
		operator fun invoke(r: Int, g: Int, b: Int, a: Int): RGBA = unclamped(r.clamp0_255(), g.clamp0_255(), b.clamp0_255(), a.clamp0_255())
        operator fun invoke(r: Int, g: Int, b: Int): RGBA = unclamped(r.clamp0_255(), g.clamp0_255(), b.clamp0_255(), 0xFF)
		operator fun invoke(rgb: Int, a: Int): RGBA = RGBA((rgb and 0xFFFFFF) or (a shl 24))
		override fun getR(v: Int): Int = RGBA(v).r
		override fun getG(v: Int): Int = RGBA(v).g
		override fun getB(v: Int): Int = RGBA(v).b
		override fun getA(v: Int): Int = RGBA(v).a
		override fun pack(r: Int, g: Int, b: Int, a: Int): Int = RGBA(r, g, b, a).value

		fun mutliplyByAlpha(v: Int, alpha: Double): Int = com.soywiz.korim.color.RGBA.pack(RGBA(v).r, RGBA(v).g, RGBA(v).b, (RGBA(v).a * alpha).toInt())
		fun depremultiply(v: RGBA): RGBA = v.asPremultiplied().depremultiplied

		fun blendComponent(c1: Int, c2: Int, factor: Double): Int = (c1 * (1.0 - factor) + c2 * factor).toInt() and 0xFF
		fun blendRGB(c1: Int, c2: Int, factor256: Int): Int = (256 - factor256).let { f1 -> ((((((c1 and 0xFF00FF) * f1) + ((c2 and 0xFF00FF) * factor256)) and 0xFF00FF00.toInt()) or ((((c1 and 0x00FF00) * f1) + ((c2 and 0x00FF00) * factor256)) and 0x00FF0000))) ushr 8 }
        fun blendRGB(c1: RGBA, c2: RGBA, factor256: Int): RGBA = RGBA(blendRGB(c1.value, c2.value, factor256))
		fun blendRGB(c1: Int, c2: Int, factor: Double): Int = blendRGB(c1, c2, (factor * 256).toInt())
		fun blendRGBAInt(c1: Int, c2: Int, factor: Double): Int = blendRGBA(RGBA(c1), RGBA(c2), factor).value
		fun blendRGBA(c1: RGBA, c2: RGBA, factor: Double): RGBA {
			val RGB = blendRGB(c1.value and 0xFFFFFF, c2.value and 0xFFFFFF, (factor * 256).toInt())
			val A = blendComponent(c1.a, c2.a, factor)
			return RGBA(RGB, A)
		}

		fun mix(dst: RGBA, src: RGBA): RGBA {
            val srcA = src.a
            return when (srcA) {
                0x000 -> dst
                0xFF -> src
                else -> RGBA(blendRGB(dst.rgb, src.rgb, srcA + 1), dst.a + srcA)
            }
        }

        fun multiply(c1: RGBA, c2: RGBA): RGBA = RGBA(
            ((c1.r * c2.r) / 0xFF),
            ((c1.g * c2.g) / 0xFF),
            ((c1.b * c2.b) / 0xFF),
            ((c1.a * c2.a) / 0xFF)
        )

        fun interpolate(src: RGBA, dst: RGBA, ratio: Double): RGBA = RGBA(
            ratio.interpolate(src.r, dst.r),
            ratio.interpolate(src.g, dst.g),
            ratio.interpolate(src.b, dst.b),
            ratio.interpolate(src.a, dst.a)
        )
    }
}

fun Double.interpolate(a: RGBA, b: RGBA): RGBA = RGBA.interpolate(a, b, this)

inline class RGBAPremultiplied(val value: Int) {
    val r: Int get() = (value ushr 0) and 0xFF
    val g: Int get() = (value ushr 8) and 0xFF
    val b: Int get() = (value ushr 16) and 0xFF
    val a: Int get() = (value ushr 24) and 0xFF

    val rf: Float get() = r.toFloat() / 255f
    val gf: Float get() = g.toFloat() / 255f
    val bf: Float get() = b.toFloat() / 255f
    val af: Float get() = a.toFloat() / 255f

    val rd: Double get() = r.toDouble() / 255.0
    val gd: Double get() = g.toDouble() / 255.0
    val bd: Double get() = b.toDouble() / 255.0
    val ad: Double get() = a.toDouble() / 255.0

    val depremultiplied: RGBA get() {
        //val A = (value ushr 24) + 1
        //val R = (((value and 0x0000FF) shl 8) / A) and 0x0000F0
        //val G = (((value and 0x00FF00) shl 8) / A) and 0x00FF00
        //val B = (((value and 0xFF0000) shl 8) / A) and 0xFF0000
        //return RGBA((value and 0x00FFFFFF.inv()) or B or G or R)

        val A = (value ushr 24)
        val A1 = A + 1
        val R = ((((value ushr 0) and 0xFF) shl 8) / A1) and 0xFF
        val G = ((((value ushr 8) and 0xFF) shl 8) / A1) and 0xFF
        val B = ((((value ushr 16) and 0xFF) shl 8) / A1) and 0xFF
        return RGBA(R, G, B, A)
    }

    val depremultipliedAccurate: RGBA get() {
        val alpha = ad
        return when (alpha) {
            0.0 -> Colors.TRANSPARENT_BLACK
            else -> {
                val ialpha = 1.0 / alpha
                RGBA((r * ialpha).toInt(), (g * ialpha).toInt(), (b * ialpha).toInt(), a)
            }
        }
    }

    val hexString: String get() = this.asNonPremultiplied().hexString
    val htmlColor: String get() = this.asNonPremultiplied().htmlColor
    val htmlStringSimple: String get() = this.asNonPremultiplied().htmlStringSimple

    override fun toString(): String = hexString

    companion object {
        fun blend(c1: RGBAPremultiplied, c2: RGBAPremultiplied): RGBAPremultiplied {
            val RB = (((c1.value and 0xFF00FF) + (c2.value and 0xFF00FF)) ushr 1) and 0xFF00FF
            val G = (((c1.value and 0x00FF00) + (c2.value and 0x00FF00)) ushr 1) and 0x00FF00
            val A = (((c1.value ushr 24) + (c2.value ushr 24)) ushr 1) and 0xFF
            return RGBAPremultiplied((A shl 24) or RB or G)
        }

        fun blend(c1: RGBAPremultiplied, c2: RGBAPremultiplied, c3: RGBAPremultiplied, c4: RGBAPremultiplied): RGBAPremultiplied {
            val RB = (((c1.value and 0xFF00FF) + (c2.value and 0xFF00FF) + (c3.value and 0xFF00FF) + (c4.value and 0xFF00FF)) ushr 2) and 0xFF00FF
            val G = (((c1.value and 0x00FF00) + (c2.value and 0x00FF00) + (c3.value and 0x00FF00) + (c4.value and 0x00FF00)) ushr 2) and 0x00FF00
            val A = (((c1.value ushr 24) + (c2.value ushr 24) + (c3.value ushr 24) + (c4.value ushr 24)) ushr 2) and 0xFF
            return RGBAPremultiplied((A shl 24) or RB or G)
        }
    }
}

fun RGBA.asPremultiplied() = RGBAPremultiplied(value)
fun RGBAPremultiplied.asNonPremultiplied() = RGBA(value)

fun RgbaArray.asPremultiplied() = RgbaPremultipliedArray(ints)
fun RgbaPremultipliedArray.asNonPremultiplied() = RgbaArray(ints)

inline class RgbaPremultipliedArray(val ints: IntArray) {
    val size: Int get() = ints.size
    operator fun get(index: Int): RGBAPremultiplied = RGBAPremultiplied(ints[index])
    operator fun set(index: Int, color: RGBAPremultiplied) = run { ints[index] = color.value }

    fun fill(value: RGBAPremultiplied, start: Int = 0, end: Int = this.size): Unit = ints.fill(value.value, start, end)

    fun premultiply(start: Int = 0, end: Int = size): RgbaArray {
        for (n in start until end) this[n] = this[n].asNonPremultiplied().premultiplied
        return this.asNonPremultiplied()
    }

    override fun toString(): String = "RgbaPremultipliedArray($size)"
}


inline class RgbaArray(val ints: IntArray) : List<RGBA> {
    companion object {
        operator fun invoke(colors: Array<RGBA>): RgbaArray = RgbaArray(colors.map { it.value }.toIntArray())
        operator fun invoke(size: Int): RgbaArray = RgbaArray(IntArray(size))
        operator fun invoke(size: Int, callback: (index: Int) -> RGBA): RgbaArray = RgbaArray(IntArray(size)).apply { for (n in 0 until size) this[n] = callback(n) }
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<RGBA> = GenericSubList(this, fromIndex, toIndex)
	override fun contains(element: RGBA): Boolean = ints.contains(element.value)
	override fun containsAll(elements: Collection<RGBA>): Boolean = elements.all { contains(it) }
	override fun indexOf(element: RGBA): Int = ints.indexOf(element.value)
	override fun lastIndexOf(element: RGBA): Int = ints.lastIndexOf(element.value)
	override fun isEmpty(): Boolean = ints.isEmpty()
	override fun iterator(): Iterator<RGBA> = listIterator(0)
	override fun listIterator(): ListIterator<RGBA> = listIterator(0)
	override fun listIterator(index: Int): ListIterator<RGBA> = GenericListIterator(this, index)

	override val size get() = ints.size
	override operator fun get(index: Int): RGBA = RGBA(ints[index])
	operator fun set(index: Int, color: RGBA) = run { ints[index] = color.value }
	fun fill(value: RGBA, start: Int = 0, end: Int = this.size): Unit = ints.fill(value.value, start, end)

    fun depremultiply(start: Int = 0, end: Int = size): RgbaPremultipliedArray {
        for (n in start until end) this[n] = this[n].asPremultiplied().depremultiplied
        return this.asPremultiplied()
    }

    override fun toString(): String = "RgbaArray($size)"
}

fun RGBA.mix(other: RGBA, ratio: Double) = RGBA.blendRGBA(this, other, ratio)

fun List<RGBA>.toRgbaArray(): RgbaArray = RgbaArray(IntArray(this.size) { this@toRgbaArray[it].value })

fun arraycopy(src: RgbaArray, srcPos: Int, dst: RgbaArray, dstPos: Int, size: Int): Unit = arraycopy(src.ints, srcPos, dst.ints, dstPos, size)

@Deprecated("", ReplaceWith("v.asPremultiplied().depremultiplied"))
fun RGBA.Companion.depremultiplyFaster(v: RGBA): RGBA = v.asPremultiplied().depremultiplied

@Deprecated("", ReplaceWith("v.asPremultiplied().depremultiplied"))
fun RGBA.Companion.depremultiplyFastest(v: RGBA): RGBA = v.asPremultiplied().depremultiplied

fun Array<RGBA>.toRgbaArray() = RgbaArray(this.size) { this@toRgbaArray[it] }
