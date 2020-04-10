package com.soywiz.korim.vector.paint

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.IntArrayList
import com.soywiz.kmem.clamp
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.vector.CycleMethod
import com.soywiz.korma.geom.*

interface Paint {
    fun transformed(m: Matrix): Paint
}

object NonePaint : Paint {
    override fun transformed(m: Matrix) = this
}

open class ColorPaint(val color: RGBA) : Paint {
    override fun transformed(m: Matrix) = this
}

/**
 * Paints a default color. For BitmapFonts, draw the original Bitmap without tinting.
 */
object DefaultPaint : ColorPaint(Colors.BLACK)

interface TransformedPaint : Paint {
    val transform: Matrix
}

enum class GradientKind {
    LINEAR, RADIAL
}

enum class GradientUnits {
    USER_SPACE_ON_USE, OBJECT_BOUNDING_BOX
}

enum class GradientInterpolationMethod {
    LINEAR, NORMAL
}

data class GradientPaint(
    val kind: GradientKind,
    val x0: Double,
    val y0: Double,
    val r0: Double,
    val x1: Double,
    val y1: Double,
    val r1: Double,
    val stops: DoubleArrayList = DoubleArrayList(),
    val colors: IntArrayList = IntArrayList(),
    val cycle: CycleMethod = CycleMethod.NO_CYCLE,
    override val transform: Matrix = Matrix(),
    val interpolationMethod: GradientInterpolationMethod = GradientInterpolationMethod.NORMAL,
    val units: GradientUnits = GradientUnits.OBJECT_BOUNDING_BOX
) : TransformedPaint {
    fun x0(m: Matrix) = m.transformX(x0, y0)
    fun y0(m: Matrix) = m.transformY(x0, y0)
    fun r0(m: Matrix) = m.transformX(r0, r0)

    fun x1(m: Matrix) = m.transformX(x1, y1)
    fun y1(m: Matrix) = m.transformY(x1, y1)
    fun r1(m: Matrix) = m.transformX(r1, r1)

    val numberOfStops get() = stops.size

    fun addColorStop(stop: Double, color: RGBA): GradientPaint = add(stop, color)

    fun add(stop: Double, color: RGBA): GradientPaint = this.apply {
        stops += stop
        colors += color.value
        return this
    }

    val gradientMatrix = Matrix().apply {
        translate(-x0, -y0)
        scale(1.0 / Point.distance(x0, y0, x1, y1).clamp(1.0, 16000.0))
        rotate(-Angle.between(x0, y0, x1, y1))
        premultiply(transform)
    }

    val gradientMatrixInv = gradientMatrix.inverted()

    // @TODO
    fun getRatioAt(x: Double, y: Double): Double {
        if (kind == GradientKind.RADIAL) {
            // @TODO
        }
        return gradientMatrix.transformX(x, y)
    }

    fun getRatioAt(x: Double, y: Double, m: Matrix): Double = getRatioAt(m.transformX(x, y), m.transformY(x, y))

    fun applyMatrix(m: Matrix): GradientPaint = GradientPaint(
        kind,
        m.transformX(x0, y0),
        m.transformY(x0, y0),
        r0,
        m.transformX(x1, y1),
        m.transformY(x1, y1),
        r1,
        DoubleArrayList(stops),
        IntArrayList(colors),
        cycle,
        Matrix(),
        interpolationMethod,
        units
    )

    override fun transformed(m: Matrix) = applyMatrix(m)

    override fun toString(): String = when (kind) {
        GradientKind.LINEAR -> "LinearGradient($x0, $y0, $x1, $y1, $stops, $colors)"
        GradientKind.RADIAL -> "RadialGradient($x0, $y0, $r0, $x1, $y1, $r1, $stops, $colors)"
    }
}

class BitmapPaint(
    val bitmap: Bitmap,
    override val transform: Matrix,
    val repeat: Boolean = false,
    val smooth: Boolean = true
) : TransformedPaint {
    val bmp32 = bitmap.toBMP32()
    override fun transformed(m: Matrix) = BitmapPaint(bitmap, Matrix().multiply(m, this.transform))
}
