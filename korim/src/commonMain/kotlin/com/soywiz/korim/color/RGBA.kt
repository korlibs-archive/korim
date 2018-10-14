package com.soywiz.korim.color

import com.soywiz.kmem.*
import com.soywiz.korio.ds.*
import com.soywiz.korio.error.*
import com.soywiz.korio.lang.*

inline fun RGBAInt(rgba: Int): Int = rgba
inline fun RGBAInt(r: Int, g: Int, b: Int, a: Int) = RGBA.pack(r, g, b, a)
inline fun RGBAInt(rgb: Int, a: Int) = rgb or (a shl 24)

//inline class RGBA(val rgba: Int) : Comparable<RGBA> {// @TODO: class inline or slow
data class RGBA(val rgba: Int) : Comparable<RGBA> {// @TODO: SUPER Extremely slow! Mark class as inline once fixes are ready
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
	operator fun minus(other: RGBA): RGBA =
		RGBA(this.r - other.r, this.g - other.g, this.b - other.b, this.a - other.a)


	// @TODO: Is c1 == c2 slow?
	override operator fun compareTo(other: RGBA): Int = this.rgba.compareTo(other.rgba)
	override fun hashCode(): Int = rgba
	override fun equals(other: Any?): Boolean = if (other is RGBA) other.rgba == this.rgba else false
	fun equals(other: RGBA): Boolean = other.rgba == this.rgba

	companion object : ColorFormat32() {
		//@JvmStatic
		//operator fun invoke(r: Int, g: Int, b: Int, a: Int) = pack(r, g, b, a)
		operator fun invoke(r: Int, g: Int, b: Int, a: Int): RGBA =
			RGBA(((r and 0xFF) shl 0) or ((g and 0xFF) shl 8) or ((b and 0xFF) shl 16) or ((a and 0xFF) shl 24))

		operator fun invoke(rgb: Int, a: Int): RGBA = RGBA((rgb and 0xFFFFFF) or (a shl 24))

		//private inline val R_SHIFT: Int get() = 0
		//private inline val G_SHIFT: Int get() = 8
		//private inline val B_SHIFT: Int get() = 16
		//private inline val A_SHIFT: Int get() = 24

		override fun getR(v: Int): Int = getFastR(v)
		override fun getG(v: Int): Int = getFastG(v)
		override fun getB(v: Int): Int = getFastB(v)
		override fun getA(v: Int): Int = getFastA(v)

		override fun pack(r: Int, g: Int, b: Int, a: Int): Int =
			((ColorFormat.clamp0_FF(r)) shl 0) or ((ColorFormat.clamp0_FF(g)) shl 8) or ((ColorFormat.clamp0_FF(b)) shl 16) or ((ColorFormat.clamp0_FF(
				a
			)) shl 24)

		//@JvmStatic
		fun getFastR(v: Int): Int = (v ushr 0) and 0xFF

		//@JvmStatic
		fun getFastG(v: Int): Int = (v ushr 8) and 0xFF

		//@JvmStatic
		fun getFastB(v: Int): Int = (v ushr 16) and 0xFF

		//@JvmStatic
		fun getFastA(v: Int): Int = (v ushr 24) and 0xFF

		//@JvmStatic
		fun getFastRf(v: Int): Float = ((v ushr 0) and 0xFF).toFloat() / 0xFF

		//@JvmStatic
		fun getFastGf(v: Int): Float = ((v ushr 8) and 0xFF).toFloat() / 0xFF

		//@JvmStatic
		fun getFastBf(v: Int): Float = ((v ushr 16) and 0xFF).toFloat() / 0xFF

		//@JvmStatic
		fun getFastAf(v: Int): Float = ((v ushr 24) and 0xFF).toFloat() / 0xFF

		//@JvmStatic
		fun getFastRd(v: Int): Double = ((v ushr 0) and 0xFF).toDouble() / 0xFF

		//@JvmStatic
		fun getFastGd(v: Int): Double = ((v ushr 8) and 0xFF).toDouble() / 0xFF

		//@JvmStatic
		fun getFastBd(v: Int): Double = ((v ushr 16) and 0xFF).toDouble() / 0xFF

		//@JvmStatic
		fun getFastAd(v: Int): Double = ((v ushr 24) and 0xFF).toDouble() / 0xFF

		//fun getRGB(v: Int): Int = v and 0xFFFFFF

		//@JvmStatic
		fun getRGB(v: Int): Int = v and 0xFFFFFF

		@Deprecated("", ReplaceWith("RGBA.premultiplyFast(v)", "com.soywiz.korim.color.RGBA"))
		//@JvmStatic
		fun multipliedByAlpha(v: RGBA): RGBA = premultiplyFast(v)

		//@JvmStatic
		fun toHexString(v: Int): String = "#%02x%02x%02x%02x".format(getFastR(v), getFastG(v), getFastB(v), getFastA(v))

		//@JvmStatic
		fun toHtmlColor(v: Int): String = "rgba(${getFastR(v)}, ${getFastG(v)}, ${getFastB(v)}, ${getFastAf(v)})"

		//@JvmStatic
		fun premultiply(v: RGBA): RGBA = premultiplyFast(v)

		//@JvmStatic
		fun premultiplyAccurate(v: Int): Int {
			val a1 = getFastA(v)
			val af = a1.toFloat() / 255f
			return packFast((getFastR(v) * af).toInt(), (getFastG(v) * af).toInt(), (getFastB(v) * af).toInt(), a1)
		}

		//@JvmStatic
		fun premultiplyFast(v: RGBA): RGBA = RGBA(premultiplyFastInt(v.rgba))

		// @TODO: kotlin-native in release make colors yellowish (a bug in LLVM optimizer?)
		fun premultiplyFastInt(v: Int): Int {
			val A = getFastA(v) + 1
			val RB = (((v and 0x00FF00FF) * A) ushr 8) and 0x00FF00FF
			val G = (((v and 0x0000FF00) * A) ushr 8) and 0x0000FF00
			return (v and 0x00FFFFFF.inv()) or RB or G
		}

		////@JvmStatic fun premultiplyFast2(v: Int): Int {
		//	val Ad = (v ushr 24).toDouble() / 255.0
		//	val RB = ((v and 0x00FF00FF) * Ad).toInt() and 0x00FF00FF
		//	val G = ((v and 0x0000FF00) * Ad).toInt() and 0x0000FF00
		//	return (v and 0x00FFFFFF.inv()) or RB or G
		//}

		//@JvmStatic
		fun mutliplyByAlpha(v: Int, alpha: Double): Int =
			com.soywiz.korim.color.RGBA.pack(getFastR(v), getFastG(v), getFastB(v), (getFastA(v) * alpha).toInt())

		//@JvmStatic
		fun depremultiply(v: RGBA): RGBA = depremultiplyFast(v)

		//@JvmStatic
		fun depremultiplyAccurate(v: RGBA): RGBA {
			val alpha = v.ad
			if (alpha == 0.0) {
				return Colors.TRANSPARENT_WHITE
			} else {
				val ialpha = 1.0 / alpha
				return RGBA(
					pack(
						(v.r * ialpha).toInt(),
						(v.g * ialpha).toInt(),
						(v.b * ialpha).toInt(),
						v.a
					)
				)
			}
		}

		fun Double.clampf1() = if (this > 1.0) 1.0 else this
		fun Int.clamp0_255() = clamp(0, 255)
		fun Int.clamp255() = if (this > 255) 255 else this

		//@JvmStatic
		fun depremultiplyFast(v: RGBA): RGBA = RGBA(depremultiplyFastInt(v.rgba))

		fun depremultiplyFastInt(v: Int): Int {
			val A = RGBA.getFastA(v)
			val alpha = A.toDouble() / 255.0
			if (alpha == 0.0) return 0
			val ialpha = 1.0 / alpha
			val R = (RGBA.getFastR(v) * ialpha).toInt().clamp255()
			val G = (RGBA.getFastG(v) * ialpha).toInt().clamp255()
			val B = (RGBA.getFastB(v) * ialpha).toInt().clamp255()
			return RGBA.packFast(R, G, B, A)
		}

		fun depremultiplyFast(data: RgbaArray, start: Int = 0, end: Int = data.size): RgbaArray = data.apply {
			val array = data.array
			for (n in start until end) array[n] = depremultiplyFastInt(array[n])
		}

		fun premultiplyFast(data: RgbaArray, start: Int = 0, end: Int = data.size): RgbaArray = data.apply {
			val array = data.array
			for (n in start until end) array[n] = premultiplyFastInt(array[n])
		}

		//@JvmStatic
		fun depremultiplyFastOld(v: Int): Int {
			val A = (v ushr 24)
			if (A == 0) return 0
			val R = ((((v ushr 0) and 0xFF) * 255) / A).clamp0_255()
			val G = ((((v ushr 8) and 0xFF) * 255) / A).clamp0_255()
			val B = ((((v ushr 16) and 0xFF) * 255) / A).clamp0_255()
			return packFast(R, G, B, A)
		}

		//@JvmStatic
		fun depremultiplyFaster(v: Int): Int {
			val A = (v ushr 24)
			val A1 = A + 1
			val R = ((((v ushr 0) and 0xFF) shl 8) / A1) and 0xFF
			val G = ((((v ushr 8) and 0xFF) shl 8) / A1) and 0xFF
			val B = ((((v ushr 16) and 0xFF) shl 8) / A1) and 0xFF
			return packFast(R, G, B, A)
		}

		//@JvmStatic
		fun depremultiplyFastest(v: Int): Int {
			val A = (v ushr 24) + 1
			val R = (((v and 0x0000FF) shl 8) / A) and 0x0000F0
			val G = (((v and 0x00FF00) shl 8) / A) and 0x00FF00
			val B = (((v and 0xFF0000) shl 8) / A) and 0xFF0000
			return (v and 0x00FFFFFF.inv()) or B or G or R
		}

		//@JvmStatic
		fun packFast(r: Int, g: Int, b: Int, a: Int) = (r shl 0) or (g shl 8) or (b shl 16) or (a shl 24)
		fun packFast(rgb: Int, a: Int): Int = (rgb and 0xFFFFFF) or (a shl 24)

		//@JvmStatic
		fun packfFast(r: Float, g: Float, b: Float, a: Float): Int =
			((r * 0xFF).toInt() shl 0) or ((g * 0xFF).toInt() shl 8) or ((b * 0xFF).toInt() shl 16) or ((a * 0xFF).toInt() shl 24)

		//@JvmStatic
		fun packRGB_A(rgb: Int, a: Int): Int = (rgb and 0xFFFFFF) or (a shl 24)

		//@JvmStatic
		fun blendComponent(c1: Int, c2: Int, factor: Double): Int = (c1 * (1.0 - factor) + c2 * factor).toInt() and 0xFF

		//@JvmStatic
		fun blendRGB(c1: Int, c2: Int, factor256: Int): Int {
			val f1 = 256 - factor256
			return ((
					((((c1 and 0xFF00FF) * f1) + ((c2 and 0xFF00FF) * factor256)) and 0xFF00FF00.toInt())
							or
							((((c1 and 0x00FF00) * f1) + ((c2 and 0x00FF00) * factor256)) and 0x00FF0000))) ushr 8
		}

		@Deprecated("", ReplaceWith("blendRGB(c1, c2, factor)", "com.soywiz.korim.color.RGBA.blendRGB"))
		//@JvmStatic
		fun blend(c1: Int, c2: Int, factor: Int): Int = blendRGB(c1, c2, factor)

		@Deprecated("", ReplaceWith("blendRGB(c1, c2, factor)", "com.soywiz.korim.color.RGBA.blendRGB"))
		//@JvmStatic
		fun blend(c1: Int, c2: Int, factor: Double): Int = blendRGB(c1, c2, factor)

		//@JvmStatic
		fun blendRGB(c1: Int, c2: Int, factor: Double): Int = blendRGB(c1, c2, (factor * 256).toInt())

		fun blendRGBAInt(c1: Int, c2: Int, factor: Double): Int = blendRGBA(RGBA(c1), RGBA(c2), factor).rgba

		//@JvmStatic
		fun blendRGBA(c1: RGBA, c2: RGBA, factor: Double): RGBA {
			val RGB = blendRGB(c1.rgba and 0xFFFFFF, c2.rgba and 0xFFFFFF, (factor * 256).toInt())
			val A = blendComponent(c1.a, c2.a, factor)
			return RGBA(packRGB_A(RGB, A))
		}

		//@JvmStatic
		fun rgbaToBgra(v: Int) =
			((v shl 16) and 0x00FF0000) or ((v shr 16) and 0x000000FF) or (v and 0xFF00FF00.toInt())

		//@JvmStatic
		private fun d2i(v: Double): Int = (ColorFormat.clampf01(v.toFloat()) * 255).toInt()

		//@JvmStatic
		private fun f2i(v: Float): Int = (ColorFormat.clampf01(v) * 255).toInt()

		//@JvmStatic
		fun packf(r: Double, g: Double, b: Double, a: Double): Int = packFast(d2i(r), d2i(g), d2i(b), d2i(a))

		//@JvmStatic
		fun packf(r: Float, g: Float, b: Float, a: Float): Int = packFast(f2i(r), f2i(g), f2i(b), f2i(a))

		//@JvmStatic
		fun packf(rgb: Int, a: Float): Int = packRGB_A(rgb, f2i(a))

		//@JvmStatic
		fun mix(dst: RGBA, src: RGBA): RGBA = RGBA(mixInt(dst.rgba, src.rgba))

		fun mixInt(dst: Int, src: Int): Int {
			val srcA = getA(src)
			return when (srcA) {
				0x000 -> dst
				0xFF -> src
				else -> RGBAInt(blendRGB(dst, src, srcA + 1), getA(dst) + srcA)
			}
		}

		//@JvmStatic
		fun interpolate(src: RGBA, dst: RGBA, ratio: Double): RGBA = RGBA(interpolateInt(src.rgba, dst.rgba, ratio))

		fun interpolateInt(src: Int, dst: Int, ratio: Double): Int = RGBA.pack(
			com.soywiz.korma.interpolation.interpolate(getR(src), getR(dst), ratio),
			com.soywiz.korma.interpolation.interpolate(getG(src), getG(dst), ratio),
			com.soywiz.korma.interpolation.interpolate(getB(src), getB(dst), ratio),
			com.soywiz.korma.interpolation.interpolate(getA(src), getA(dst), ratio)
		)

		//@JvmStatic
		fun multiply(c1: RGBA, c2: RGBA): RGBA = RGBA(multiplyInt(c1.rgba, c2.rgba))

		fun multiplyInt(c1: Int, c2: Int): Int = RGBAInt(
			clamp0_FF((RGBA.getR(c1) * RGBA.getR(c2)) / 0xFF),
			clamp0_FF((RGBA.getG(c1) * RGBA.getG(c2)) / 0xFF),
			clamp0_FF((RGBA.getB(c1) * RGBA.getB(c2)) / 0xFF),
			clamp0_FF((RGBA.getA(c1) * RGBA.getA(c2)) / 0xFF)
		)

		//@JvmStatic
		fun blendRGBAFastAlreadyPremultiplied_05(c1: Int, c2: Int): Int {
			//val R1 = getFastR(c1)
			//val G1 = getFastG(c1)
			//val B1 = getFastB(c1)
			//val A1 = getFastA(c1)
			//
			//val R2 = getFastR(c2)
			//val G2 = getFastG(c2)
			//val B2 = getFastB(c2)
			//val A2 = getFastA(c2)
			//
			//return RGBA.pack((R1 + R2) / 2, (G1 + G2) / 2, (B1 + B2) / 2, (A1 + A2) / 2)

			val RB = (((c1 and 0xFF00FF) + (c2 and 0xFF00FF)) ushr 1) and 0xFF00FF
			val G = (((c1 and 0x00FF00) + (c2 and 0x00FF00)) ushr 1) and 0x00FF00
			val A = (((c1 ushr 24) + (c2 ushr 24)) ushr 1) and 0xFF
			return (A shl 24) or RB or G
		}

		//@JvmStatic
		fun blendRGBAFastAlreadyPremultiplied_05(c1: Int, c2: Int, c3: Int, c4: Int): Int {
			val RB =
				(((c1 and 0xFF00FF) + (c2 and 0xFF00FF) + (c3 and 0xFF00FF) + (c4 and 0xFF00FF)) ushr 2) and 0xFF00FF
			val G =
				(((c1 and 0x00FF00) + (c2 and 0x00FF00) + (c3 and 0x00FF00) + (c4 and 0x00FF00)) ushr 2) and 0x00FF00
			val A = (((c1 ushr 24) + (c2 ushr 24) + (c3 ushr 24) + (c4 ushr 24)) ushr 2) and 0xFF
			return (A shl 24) or RB or G
		}

		////@JvmStatic fun downScaleBy2AlreadyPremultiplied(
		//	dstData: IntArray, dstOffset: Int, dstStep: Int,
		//	srcData: IntArray, srcOffset: Int, srcStep: Int,
		//	count: Int
		//) {
		//	var src = srcOffset
		//	var dst = dstOffset
		//	if (count > 0) {
		//		for (n in 0 until count) {
		//			var c1 = srcData[src]
		//			val c2 = srcData[src + srcStep]
		//			dstData[dst] = RGBA.blendRGBAFastAlreadyPremultiplied_05(c1, c2)
		//			//dstData[dst] = c1
		//			src += srcStep + srcStep
		//			dst += dstStep
		//			c1 = c2
		//		}
		//	}
		//}

		fun toString(c: Int): String = "RGBA(${getR(c)},${getG(c)},${getB(c)},${getAf(c)})"
	}
}


//inline class RgbaArray(val array: IntArray) : List<RGBA> { // @TODO: class inline or slow!
class RgbaArray(val array: IntArray) : List<RGBA> {
	override fun subList(fromIndex: Int, toIndex: Int): List<RGBA> = SubListGeneric(this, fromIndex, toIndex)
	override fun contains(element: RGBA): Boolean = array.contains(element.rgba)
	override fun containsAll(elements: Collection<RGBA>): Boolean = elements.all { contains(it) }
	override fun indexOf(element: RGBA): Int = array.indexOf(element.rgba)
	override fun lastIndexOf(element: RGBA): Int = array.lastIndexOf(element.rgba)
	override fun isEmpty(): Boolean = array.isEmpty()
	override fun iterator(): Iterator<RGBA> = listIterator(0)
	override fun listIterator(): ListIterator<RGBA> = listIterator(0)
	override fun listIterator(index: Int): ListIterator<RGBA> = GenericListIterator(this, index)

	//constructor(size: Int) : this(IntArray(size))
	companion object {
		operator fun invoke(size: Int): RgbaArray = RgbaArray(IntArray(size))
		operator fun invoke(size: Int, callback: (index: Int) -> RGBA): RgbaArray = RgbaArray(IntArray(size)).apply { for (n in 0 until size) this[n] = callback(n) }
		fun genInt(size: Int, callback: (index: Int) -> Int): RgbaArray = RgbaArray(IntArray(size)).apply { for (n in 0 until size) this.array[n] = callback(n) }

		/**
		 * java.lang.VerifyError: Bad type on operand stack
		 * Exception Details:
		 * Location:
		 */
		//inline operator fun invoke(size: Int, callback: (index: Int) -> RGBA): RgbaArray = RgbaArray(IntArray(size)).apply { for (n in 0 until size) this[n] = callback(n) }
	}

	override val size get() = array.size
	override operator fun get(index: Int): RGBA = RGBA(array[index])
	operator fun set(index: Int, color: RGBA) = run { array[index] = color.rgba }
	fun fill(value: RGBA, start: Int = 0, end: Int = this.size): Unit = array.fill(value.rgba, start, end)

	override fun toString(): String = "RgbaArray($size)"
}

fun RGBA.mix(other: RGBA, ratio: Double) = RGBA.blendRGBA(this, other, ratio)

/*
java.lang.VerifyError: Bad type on operand stack
Exception Details:
  Location:
    com/soywiz/korim/color/RGBAKt$toRgbaArray$2.invoke(I)I @6: invokevirtual
  Reason:
    Type 'com/soywiz/korim/color/RGBA' (current frame, stack[0]) is not assignable to 'java/lang/Integer'
  Current Frame:
    bci: @6
    flags: { }
    locals: { 'com/soywiz/korim/color/RGBAKt$toRgbaArray$2', integer }
    stack: { 'com/soywiz/korim/color/RGBA' }
  Bytecode:
 */
//fun Collection<RGBA>.toRgbaArray(): RgbaArray = RgbaArray(this.size).apply {
//	for ((index, it) in this@toRgbaArray.withIndex()) this[index] = it
//}


//fun Collection<RGBA>.toRgbaArray(): RgbaArray {
//	val out = RgbaArray(this.size)
//	for ((index, it) in this@toRgbaArray.withIndex()) out[index] = it
//	return out
//}

//un List<RGBA>.toRgbaArray(): RgbaArray {
//	val out = RgbaArray(IntArray(this.size))
//	for (n in 0 until size) out[n] = this[n]
//	return out
//

fun List<RGBA>.toRgbaArray(): RgbaArray = RgbaArray(IntArray(this.size) { this@toRgbaArray[it].rgba })

fun arraycopy(src: RgbaArray, srcPos: Int, dst: RgbaArray, dstPos: Int, size: Int): Unit = arraycopy(src.array, srcPos, dst.array, dstPos, size)

@Deprecated("")
fun RGBA.Companion.getR(v: RGBA): Int = v.r
@Deprecated("")
fun RGBA.Companion.getG(v: RGBA): Int = v.g
@Deprecated("")
fun RGBA.Companion.getB(v: RGBA): Int = v.b
@Deprecated("")
fun RGBA.Companion.getA(v: RGBA): Int = v.a

fun RGBA.Companion.toHexString(v: RGBA): String = v.hexString
fun RGBA.Companion.toHtmlColor(v: RGBA): String = v.htmlColor

fun RGBA.Companion.depremultiplyFaster(v: RGBA): RGBA = RGBA(RGBA.depremultiplyFaster(v.toInt()))
fun RGBA.Companion.depremultiplyFastest(v: RGBA): RGBA = RGBA(RGBA.depremultiplyFastest(v.toInt()))

/**
 * java.lang.VerifyError: Bad type on operand stack
 * Exception Details:
 * Location:
 * com/soywiz/korim/color/RGBAKt$toRgbaArray$1.invoke(I)I @6: invokevirtual
 */
fun Array<RGBA>.toRgbaArray() = RgbaArray(this.size) { this@toRgbaArray[it] }

/**
 * java.lang.VerifyError: Bad type on operand stack
 * Exception Details:
 * Location:
 * com/soywiz/korim/color/RGBAKt.toRgbaArray([Lcom/soywiz/korim/color/RGBA;)Lcom/soywiz/korim/color/RgbaArray; @30: invokevirtual
 */
//fun Array<RGBA>.toRgbaArray(): RgbaArray {
//	val out = RgbaArray(this.size)
//	for (n in 0 until size) out[n] = this[n]
//	return out
//}

/*
inline class Rgba(val rgba: Int) {
	//data class Color(val rgba: Int) {
	val r: Int get() = (rgba ushr 0) and 0xFF
	val g: Int get() = (rgba ushr 8) and 0xFF
	val b: Int get() = (rgba ushr 16) and 0xFF
	val a: Int get() = (rgba ushr 24) and 0xFF

	operator fun plus(other: Rgba) = Color(this.r + other.r, this.g + other.g, this.b + other.b, this.a + other.a)
	operator fun minus(other: Rgba) = Color(this.r - other.r, this.g - other.g, this.b - other.b, this.a - other.a)
}

fun Color(r: Int, g: Int, b: Int, a: Int = 0xFF) = Rgba(((r and 0xFF) shl 0) or ((g and 0xFF) shl 8) or ((b and 0xFF) shl 16) or ((a and 0xFF) shl 24))

fun Rgba.withR(r: Int) = Color(r, g, b, a)
fun Rgba.withG(g: Int) = Color(r, g, b, a)
fun Rgba.withB(b: Int) = Color(r, g, b, a)
fun Rgba.withA(a: Int) = Color(r, g, b, a)



fun test(color: Rgba) {
	val array = RgbaArray(100)
	array[0] = Rgba()
}
*/
