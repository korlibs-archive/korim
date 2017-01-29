package com.soywiz.korim.geom

class Matrix2d(
	@JvmField var a: Double = 1.0,
	@JvmField var b: Double = 0.0,
	@JvmField var c: Double = 0.0,
	@JvmField var d: Double = 1.0,
	@JvmField var tx: Double = 0.0,
	@JvmField var ty: Double = 0.0
) {
	fun setTo(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double): Matrix2d {
		this.a = a
		this.b = b
		this.c = c
		this.d = d
		this.tx = tx
		this.ty = ty
		return this
	}

	fun copyFrom(that: Matrix2d) {
		setTo(that.a, that.b, that.c, that.d, that.tx, that.ty)
	}

	fun rotate(theta: Double) = this.apply {
		val cos = Math.cos(theta)
		val sin = Math.sin(theta)

		val a1 = a * cos - b * sin
		b = a * sin + b * cos
		a = a1

		val c1 = c * cos - d * sin
		d = c * sin + d * cos
		c = c1

		val tx1 = tx * cos - ty * sin
		ty = tx * sin + ty * cos
		tx = tx1
	}

	fun skew(skewX: Double, skewY: Double): Matrix2d {
		val sinX = Math.sin(skewX)
		val cosX = Math.cos(skewX)
		val sinY = Math.sin(skewY)
		val cosY = Math.cos(skewY)

		return this.setTo(
			a * cosY - b * sinX,
			a * sinY + b * cosX,
			c * cosY - d * sinX,
			c * sinY + d * cosX,
			tx * cosY - ty * sinX,
			tx * sinY + ty * cosX
		)
	}

	fun scale(sx: Double, sy: Double) = setTo(a * sx, b * sx, c * sy, d * sy, tx * sx, ty * sy)
	fun prescale(sx: Double, sy: Double) = setTo(a * sx, b * sx, c * sy, d * sy, tx, ty)
	fun translate(dx: Double, dy: Double) = this.apply { this.tx += dx; this.ty += dy }
	fun pretranslate(dx: Double, dy: Double) = this.apply { tx += a * dx + c * dy; ty += b * dx + d * dy }

	fun prerotate(theta: Double) = this.apply {
		val m = Matrix2d()
		m.rotate(theta)
		this.premulitply(m)
	}

	fun preskew(skewX: Double, skewY: Double) = this.apply {
		val m = Matrix2d()
		m.skew(skewX, skewY)
		this.premulitply(m)
	}

	fun premulitply(m: Matrix2d) = this.premulitply(m.a, m.b, m.c, m.d, m.tx, m.ty)

	fun premulitply(la: Double, lb: Double, lc: Double, ld: Double, ltx: Double, lty: Double): Matrix2d = setTo(
		la * a + lb * c,
		la * b + lb * d,
		lc * a + ld * c,
		lc * b + ld * d,
		ltx * a + lty * c + tx,
		ltx * b + lty * d + ty
	)

	fun multiply(l: Matrix2d, r: Matrix2d): Matrix2d = setTo(
		l.a * r.a + l.b * r.c,
		l.a * r.b + l.b * r.d,
		l.c * r.a + l.d * r.c,
		l.c * r.b + l.d * r.d,
		l.tx * r.a + l.ty * r.c + r.tx,
		l.tx * r.b + l.ty * r.d + r.ty
	)

	fun transform(px: Double, py: Double, out: Vector2 = Vector2()): Vector2 = out.setTo(transformX(px, py), transformY(px, py))

	fun transformX(px: Double, py: Double): Double = this.a * px + this.c * py + this.tx
	fun transformY(px: Double, py: Double): Double = this.d * py + this.b * px + this.ty

	fun transformXf(px: Double, py: Double): Float = (this.a * px + this.c * py + this.tx).toFloat()
	fun transformYf(px: Double, py: Double): Float = (this.d * py + this.b * px + this.ty).toFloat()

	override fun toString(): String {
		return "Matrix2d(a=$a, b=$b, c=$c, d=$d, tx=$tx, ty=$ty)"
	}

	fun setToIdentity() = setTo(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

	fun setToInverse(matrixToInvert: Matrix2d = this): Matrix2d {
		val m = matrixToInvert
		val norm = m.a * m.d - m.b * m.c

		if (norm == 0.0) {
			setTo(0.0, 0.0, 0.0, 0.0, -m.tx, -m.ty)
		} else {
			val inorm = 1.0 / norm
			d = m.a * inorm
			a = m.d * inorm
			b = m.b * -inorm
			c = m.c * -inorm
			ty = -b * m.tx - d * m.ty
			tx = -a * m.tx - c * m.ty
		}

		return this
	}

	fun identity() = setTo(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

	fun setTransform(x: Double, y: Double, scaleX: Double, scaleY: Double, rotation: Double, skewX: Double, skewY: Double): Matrix2d {
		if (skewX == 0.0 && skewY == 0.0) {
			if (rotation == 0.0) {
				this.setTo(scaleX, 0.0, 0.0, scaleY, x, y)
			} else {
				val cos = Math.cos(rotation)
				val sin = Math.sin(rotation)
				this.setTo(cos * scaleX, sin * scaleY, -sin * scaleX, cos * scaleY, x, y)
			}
		} else {
			identity()
			scale(scaleX, scaleY)
			skew(skewX, skewY)
			rotate(rotation)
			translate(x, y)
		}
		return this
	}

	fun clone() = Matrix2d(a, b, c, d, tx, ty)

	data class Transform(
		var x: Double = 0.0, var y: Double = 0.0,
		var scaleX: Double = 0.0, var scaleY: Double = 0.0,
		var skewX: Double = 0.0, var skewY: Double = 0.0,
		var rotation: Double = 0.0
	) {
		fun setMatrix(matrix: Matrix2d): Transform {
			val PI_4 = Math.PI / 4.0
			this.x = matrix.tx
			this.y = matrix.ty

			this.skewX = Math.atan(-matrix.c / matrix.d)
			this.skewY = Math.atan(matrix.b / matrix.a)

			// Faster isNaN
			if (this.skewX != this.skewX) this.skewX = 0.0
			if (this.skewY != this.skewY) this.skewY = 0.0

			this.scaleY = if (this.skewX > -PI_4 && this.skewX < PI_4) matrix.d / Math.cos(this.skewX) else -matrix.c / Math.sin(this.skewX)
			this.scaleX = if (this.skewY > -PI_4 && this.skewY < PI_4) matrix.a / Math.cos(this.skewY) else matrix.b / Math.sin(this.skewY)

			if (Math.abs(this.skewX - this.skewY) < 0.0001) {
				this.rotation = this.skewX
				this.skewX = 0.0
				this.skewY = 0.0
			} else {
				this.rotation = 0.0
			}

			return this
		}

		fun toMatrix(out: Matrix2d = Matrix2d()): Matrix2d = out.setTransform(x, y, scaleX, scaleY, rotation, skewX, skewY)

		fun copyFrom(that: Transform) = setTo(that.x, that.y, that.scaleX, that.scaleY, that.rotation, that.skewX, that.skewY)

		fun setTo(x: Double, y: Double, scaleX: Double, scaleY: Double, rotation: Double, skewX: Double, skewY: Double): Transform {
			this.x = x
			this.y = y
			this.scaleX = scaleX
			this.scaleY = scaleY
			this.rotation = rotation
			this.skewX = skewX
			this.skewY = skewY
			return this
		}

		fun clone() = Transform().copyFrom(this)
	}
}