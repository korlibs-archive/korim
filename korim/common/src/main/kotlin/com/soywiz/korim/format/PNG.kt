package com.soywiz.korim.format

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmap8
import com.soywiz.korim.color.RGB
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.KorioNative.SyncCompression
import com.soywiz.korio.lang.toByteArray
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.convertRangeClamped
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max

object PNG : ImageFormat("png") {
	const val MAGIC1 = 0x89504E47.toInt()
	const val MAGIC2 = 0x0D0A1A0A.toInt()

	data class InterlacedPass(
		val startingRow: Int, val startingCol: Int,
		val rowIncrement: Int, val colIncrement: Int,
		val blockHeight: Int, val blockWidth: Int
	) {
		val colIncrementShift = log2(colIncrement.toDouble()).toInt()
	}

	val InterlacedPasses = listOf(
		InterlacedPass(0, 0, 8, 8, 8, 8),
		InterlacedPass(0, 4, 8, 8, 8, 4),
		InterlacedPass(4, 0, 8, 4, 4, 4),
		InterlacedPass(0, 2, 4, 4, 4, 2),
		InterlacedPass(2, 0, 4, 2, 2, 2),
		InterlacedPass(0, 1, 2, 2, 2, 1),
		InterlacedPass(1, 0, 2, 1, 1, 1)
	)

	val NormalPasses = listOf(
		InterlacedPass(0, 0, 1, 1, 1, 1)
	)

	enum class Colorspace(val id: Int) {
		GRAYSCALE(0),
		RGB(2),
		INDEXED(3),
		GRAYSCALE_ALPHA(4),
		RGBA(6);

		companion object {
			val BY_ID = values().associateBy { it.id }
		}
	}

	class Header(
		val width: Int,
		val height: Int,
		val bitsPerChannel: Int,
		val colorspace: Colorspace, // 0=grayscale, 2=RGB, 3=Indexed, 4=grayscale+alpha, 6=RGBA
		val compressionmethod: Int, // 0
		val filtermethod: Int,
		val interlacemethod: Int
	) {
		val bytes = when (colorspace) {
			Colorspace.GRAYSCALE -> 1
			Colorspace.INDEXED -> 1
			Colorspace.GRAYSCALE_ALPHA -> 2
			Colorspace.RGB -> 3
			Colorspace.RGBA -> 4
			else -> 1
		}
		val stride = width * bytes
	}

	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = try {
		val header = readCommon(s, readHeader = true) as Header
		ImageInfo().apply {
			this.width = header.width
			this.height = header.height
			this.bitsPerPixel = header.bitsPerChannel
		}
	} catch (t: Throwable) {
		null
	}

	override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
		val bitmap = image.mainBitmap
		val width = bitmap.width
		val height = bitmap.height
		s.write32_be(MAGIC1)
		s.write32_be(MAGIC2)

		fun writeChunk(name: String, data: ByteArray) {
			val nameBytes = name.toByteArray().copyOf(4)

			val crc = CRC32()
			crc.update(nameBytes)
			crc.update(data)

			s.write32_be(data.size)
			s.writeBytes(nameBytes)
			s.writeBytes(data)
			s.write32_be(crc.value.toInt()) // crc32!
		}

		val level = props.quality.convertRangeClamped(0.0, 1.0, 0.0, 9.0).toInt()

		fun compress(data: ByteArray): ByteArray {
			if (level == 0) {
				//if (false) {
				val adler32 = Adler32()
				//if (false) {
				//val data = ByteArray(0x15)
				val blocks = ceil(data.size.toDouble() / 0xFFFF.toDouble()).toInt()
				val lastBlockSize = data.size % 0xFFFF
				val out = ByteArray(2 + 4 + data.size + blocks * 5)
				var upos = 0
				var pos = 2
				out.write8(0, 0x78)
				out.write8(1, 0x01)
				for (n in 0 until blocks) {
					val last = n == blocks - 1
					val size = if (last) lastBlockSize else 0xFFFF
					out.write8(pos, if (last) 1 else 0)
					out.write16_le(pos + 1, size)
					out.write16_le(pos + 3, size.inv())
					arraycopy(data, upos, out, pos + 5, size)
					pos += 5 + size
					upos += size
				}

				//adler32.update(out, 0, pos)
				adler32.update(data, 0, data.size)
				out.write32_be(pos + 0, adler32.value.toInt())
				return out
			} else {
				return SyncCompression.deflate(data, level)
			}
		}

		fun writeChunk(name: String, initialCapacity: Int = 4096, callback: SyncStream.() -> Unit) {
			return writeChunk(name, MemorySyncStreamToByteArray(initialCapacity) { callback() })
		}

		fun writeChunkCompressed(name: String, initialCapacity: Int = 4096, callback: SyncStream.() -> Unit) {
			return writeChunk(name, compress(MemorySyncStreamToByteArray(initialCapacity) { callback() }))
		}

		fun writeHeader(colorspace: Colorspace) {
			writeChunk("IHDR", initialCapacity = 13) {
				write32_be(width)
				write32_be(height)
				write8(8) // bits
				write8(colorspace.id) // colorspace
				write8(0) // compressionmethod
				write8(0) // filtermethod
				write8(0) // interlacemethod
			}
		}

		when (bitmap) {
			is Bitmap8 -> {
				writeHeader(Colorspace.INDEXED)
				writeChunk("PLTE", initialCapacity = bitmap.palette.size * 3) {
					for (c in bitmap.palette) {
						write8(RGBA.getR(c))
						write8(RGBA.getG(c))
						write8(RGBA.getB(c))
					}
				}
				writeChunk("tRNS", initialCapacity = bitmap.palette.size * 1) {
					for (c in bitmap.palette) {
						write8(RGBA.getA(c))
					}
				}

				val out = ByteArray(height + width * height)
				var pos = 0
				for (y in 0 until height) {
					out.write8(pos++, 0)
					val index = bitmap.index(0, y)
					arraycopy(bitmap.data, index, out, pos, width)
					pos += width
				}
				writeChunk("IDAT", compress(out))
			}
			is Bitmap32 -> {
				writeHeader(Colorspace.RGBA)

				val out = ByteArray(height + width * height * 4)
				var pos = 0
				for (y in 0 until height) {
					out.write8(pos++, 0) // no filter
					val index = bitmap.index(0, y)
					if (bitmap.premult) {
						for (x in 0 until width) {
							out.write32_le(pos, RGBA.depremultiplyFast(bitmap.data[index + x]))
							pos += 4
						}
					} else {
						for (x in 0 until width) {
							out.write32_le(pos, bitmap.data[index + x])
							pos += 4
						}
					}
				}

				writeChunk("IDAT", compress(out))
			}
		}

		writeChunk("IEND", initialCapacity = 0) {
		}
	}

	private fun readCommon(s: SyncStream, readHeader: Boolean): Any? {
		if (s.readS32_be() != MAGIC1) throw IllegalArgumentException("Invalid PNG file")
		s.readS32_be() // magic continuation

		var pheader: Header? = null
		val pngdata = ByteArrayBuilder()
		val rgbPalette = UByteArray(3 * 0x100)
		val aPalette = UByteArray(ByteArray(0x100) { -1 })
		var paletteCount = 0

		fun SyncStream.readChunk() {
			val length = readS32_be()
			val type = readStringz(4)
			val data = readStream(length.toLong())
			val crc = readS32_be()

			when (type) {
				"IHDR" -> {
					pheader = data.run {
						Header(
							width = readS32_be(),
							height = readS32_be(),
							bitsPerChannel = readU8(),
							colorspace = Colorspace.BY_ID[readU8()] ?: Colorspace.RGBA,
							compressionmethod = readU8(),
							filtermethod = readU8(),
							interlacemethod = readU8()
						)
					}

					val header = pheader!!
				}
				"PLTE" -> {
					paletteCount = max(paletteCount, data.length.toInt() / 3)
					data.read(rgbPalette.data, 0, data.length.toInt())
				}
				"tRNS" -> {
					paletteCount = max(paletteCount, data.length.toInt())
					data.read(aPalette.data, 0, data.length.toInt())
				}
				"IDAT" -> {
					pngdata.append(data.readAll())
				}
				"IEND" -> {
				}
			}
			//println(type)
		}

		while (!s.eof) {
			s.readChunk()
			if (readHeader && pheader != null) return pheader
		}

		val header = pheader ?: throw IllegalArgumentException("PNG without header!")
		val width = header.width
		val height = header.height

		val datab = ByteArray((1 + width) * height * header.bytes)

		SyncCompression.inflateTo(pngdata.toByteArray(), datab)

		val data = datab.openSync()
		val context = DecodingContext(header)
		val bpp = context.header.bytes
		val row32 = context.row32

		val bmp = when {
			header.bytes == 1 -> Bitmap8(width, height, palette = (0 until paletteCount).map { RGBA(rgbPalette[it * 3 + 0], rgbPalette[it * 3 + 1], rgbPalette[it * 3 + 2], aPalette[it]) }.toIntArray())
			else -> Bitmap32(width, height)
		}
		val bmp8 = bmp as? Bitmap8?
		val bmp32 = bmp as? Bitmap32?
		val passes = when (header.interlacemethod) {
			1 -> InterlacedPasses
			else -> NormalPasses
		}

		for (pass in passes) {
			for (row in pass.startingRow until height step pass.rowIncrement) {
				val col = pass.startingCol
				val colIncrement = pass.colIncrement
				val pixelsInThisRow = width ushr pass.colIncrementShift
				val bytesInThisRow = (pixelsInThisRow * header.bytes)
				val filter = data.readU8()
				val currentRow = context.currentRow
				val lastRow = context.lastRow
				data.readExact(currentRow.data, 0, bytesInThisRow)
				when {
					bmp8 != null -> {
						applyFilter(filter, lastRow, currentRow, header.bytes)
						bmp8.setRowChunk(col, row, currentRow.data, width, colIncrement)
					}
					bmp32 != null -> {
						applyFilter(filter, lastRow, currentRow, bpp, bytesInThisRow)
						when (bpp) {
							3 -> RGB.decode(currentRow.data, 0, row32, 0, pixelsInThisRow)
							4 -> RGBA.decode(currentRow.data, 0, row32, 0, pixelsInThisRow)
							else -> TODO("Bytes: $bpp")
						}
						bmp32.setRowChunk(col, row, row32, width, colIncrement)
					}
				}
				context.swapRows()
			}
		}

		return bmp
	}

	class DecodingContext(val header: Header) {
		var lastRow = UByteArray(header.stride)
		var currentRow = UByteArray(header.stride)
		val row32 = IntArray(header.width)

		fun swapRows() {
			val temp = currentRow
			currentRow = lastRow
			lastRow = temp
		}
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData =
		ImageData(listOf(ImageFrame(readCommon(s, readHeader = false) as Bitmap)))

	fun paethPredictor(a: Int, b: Int, c: Int): Int {
		val p = a + b - c
		val pa = abs(p - a)
		val pb = abs(p - b)
		val pc = abs(p - c)
		return if ((pa <= pb) && (pa <= pc)) a else if (pb <= pc) b else c
	}

	fun applyFilter(filter: Int, p: UByteArray, c: UByteArray, bpp: Int, size: Int = c.size) {
		when (filter) {
			0 -> Unit
			1 -> for (n in bpp until size) c[n] += c[n - bpp]
			2 -> for (n in 0 until size) c[n] += p[n]
			3 -> {
				for (n in 0 until bpp) c[n] += p[n] / 2
				for (n in bpp until size) c[n] += (c[n - bpp] + p[n]) / 2
			}
			4 -> {
				for (n in 0 until bpp) c[n] += p[n]
				for (n in bpp until size) c[n] += paethPredictor(c[n - bpp], p[n], p[n - bpp])
			}
			else -> TODO("Filter: $filter")
		}
	}
}

class Adler32 {

	private var s1 = 1
	private var s2 = 0

	val value: Int
		get() = s2 shl 16 or s1

	fun reset(init: Int) {
		s1 = init shr 0 and 0xffff
		s2 = init shr 16 and 0xffff
	}

	fun reset() {
		s1 = 1
		s2 = 0
	}

	fun update(buf: ByteArray, index: Int, len: Int) {
		var index = index
		var len = len
		if (len == 1) {
			s1 += buf[index++].toInt() and 0xff
			s2 += s1
			s1 %= BASE
			s2 %= BASE
			return
		}

		var len1 = len / NMAX
		val len2 = len % NMAX
		while (len1-- > 0) {
			var k = NMAX
			len -= k
			while (k-- > 0) {
				s1 += buf[index++].toInt() and 0xff
				s2 += s1
			}
			s1 %= BASE
			s2 %= BASE
		}

		var k = len2
		len -= k
		while (k-- > 0) {
			s1 += buf[index++].toInt() and 0xff
			s2 += s1
		}
		s1 %= BASE
		s2 %= BASE
	}

	fun copy(): Adler32 {
		val foo = Adler32()
		foo.s1 = this.s1
		foo.s2 = this.s2
		return foo
	}

	companion object {
		// largest prime smaller than 65536
		private val BASE = 65521
		// NMAX is the largest n such that 255n(n+1)/2 + (n+1)(BASE-1) <= 2^32-1
		private val NMAX = 5552

		// The following logic has come from zlib.1.2.
		internal fun combine(adler1: Long, adler2: Long, len2: Long): Long {
			val BASEL = BASE.toLong()
			val rem: Long = len2 % BASEL
			var sum1: Long = adler1 and 0xffffL
			var sum2: Long = rem * sum1
			sum2 %= BASEL // MOD(sum2);
			sum1 += (adler2 and 0xffffL) + BASEL - 1
			sum2 += (adler1 shr 16 and 0xffffL) + (adler2 shr 16 and 0xffffL) + BASEL - rem
			if (sum1 >= BASEL) sum1 -= BASEL
			if (sum1 >= BASEL) sum1 -= BASEL
			if (sum2 >= BASEL shl 1) sum2 -= BASEL shl 1
			if (sum2 >= BASEL) sum2 -= BASEL
			return sum1 or (sum2 shl 16)
		}
	}
}

class CRC32 {
	/*
	 *  The following logic has come from RFC1952.
     */
	private var v = 0

	val value: Int
		get() = v

	fun update(buf: ByteArray, index: Int = 0, len: Int = buf.size - index) {
		var index = index
		var len = len
		//int[] crc_table = CRC32.crc_table;
		var c = v.inv()
		while (--len >= 0) {
			c = crc_table!![c xor buf[index++].toInt() and 0xff] xor c.ushr(8)
		}
		v = c.inv()
	}

	fun reset() = run { v = 0 }
	fun reset(vv: Int) = run { v = vv }

	fun copy(): CRC32 {
		val foo = CRC32()
		foo.v = this.v
		return foo
	}

	companion object {
		private var crc_table: IntArray = IntArray(256)

		init {
			for (n in 0 until 0x100) {
				var c = n
				var k = 8
				while (--k >= 0) {
					if (c and 1 != 0) {
						c = -0x12477ce0 xor c.ushr(1)
					} else {
						c = c.ushr(1)
					}
				}
				crc_table[n] = c
			}
		}

		// The following logic has come from zlib.1.2.
		private val GF2_DIM = 32

		internal fun combine(crc1: Long, crc2: Long, len2: Long): Long {
			var crc1 = crc1
			var len2 = len2
			var row: Long
			val even = LongArray(GF2_DIM)
			val odd = LongArray(GF2_DIM)

			// degenerate case (also disallow negative lengths)
			if (len2 <= 0) return crc1

			// put operator for one zero bit in odd
			odd[0] = 0xedb88320L          // CRC-32 polynomial
			row = 1
			for (n in 1 until GF2_DIM) {
				odd[n] = row
				row = row shl 1
			}

			// put operator for two zero bits in even
			gf2_matrix_square(even, odd)

			// put operator for four zero bits in odd
			gf2_matrix_square(odd, even)

			// apply len2 zeros to crc1 (first square will put the operator for one
			// zero byte, eight zero bits, in even)
			do {
				// apply zeros operator for this bit of len2
				gf2_matrix_square(even, odd)
				if (len2 and 1 != 0L) crc1 = gf2_matrix_times(even, crc1)
				len2 = len2 shr 1

				// if no more bits set, then done
				if (len2 == 0L) break

				// another iteration of the loop with odd and even swapped
				gf2_matrix_square(odd, even)
				if (len2 and 1 != 0L) crc1 = gf2_matrix_times(odd, crc1)
				len2 = len2 shr 1

				// if no more bits set, then done
			} while (len2 != 0L)

			/* return combined crc */
			crc1 = crc1 xor crc2
			return crc1
		}

		private fun gf2_matrix_times(mat: LongArray, vec: Long): Long {
			var vec = vec
			var sum: Long = 0
			var index = 0
			while (vec != 0L) {
				if (vec and 1 != 0L)
					sum = sum xor mat[index]
				vec = vec shr 1
				index++
			}
			return sum
		}

		internal fun gf2_matrix_square(square: LongArray, mat: LongArray) {
			for (n in 0 until GF2_DIM)
				square[n] = gf2_matrix_times(mat, mat[n])
		}

		val crC32Table: IntArray
			get() {
				val tmp = IntArray(crc_table.size)
				arraycopy(crc_table, 0, tmp, 0, tmp.size)
				return tmp
			}
	}
}
