package com.soywiz.kimage.format

import com.soywiz.kimage.bitmap.Bitmap
import com.soywiz.kimage.bitmap.Bitmap32
import com.soywiz.kimage.bitmap.Bitmap8
import com.soywiz.kimage.color.RGBA
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.UByteArray
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterInputStream
import java.util.zip.InflaterInputStream

object PNG : ImageFormat() {
	const val MAGIC1 = 0x89504E47.toInt()
	const val MAGIC2 = 0x0D0A1A0A.toInt()

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
		val bits: Int,
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
	}

	override fun check(s: SyncStream): Boolean {
		val magic = s.readS32_be()
		return magic == MAGIC1
	}

	override fun write(bitmap: Bitmap, s: SyncStream) {
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

		val deflater = Deflater(1)

		fun compress(data: ByteArray): ByteArray {
			return DeflaterInputStream(ByteArrayInputStream(data), deflater).readBytes()
		}

		fun writeChunk(name: String, callback: SyncStream.() -> Unit) {
			return writeChunk(name, MemorySyncStream().apply { callback() }.toByteArray())
		}

		fun writeChunkCompressed(name: String, callback: SyncStream.() -> Unit) {
			return writeChunk(name, compress(MemorySyncStream().apply { callback() }.toByteArray()))
		}

		fun writeHeader(colorspace: Colorspace) {
			writeChunk("IHDR") {
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
				writeChunk("PLTE") {
					for (c in bitmap.palette) {
						write8(RGBA.getR(c))
						write8(RGBA.getG(c))
						write8(RGBA.getB(c))
					}
				}
				writeChunk("tRNS") {
					for (c in bitmap.palette) {
						write8(RGBA.getA(c))
					}
				}
				writeChunkCompressed("IDAT") {
					for (y in 0 until height) {
						write8(0) // no filter
						for (x in 0 until width) {
							write8(bitmap[x, y])
						}
					}
				}
			}
			is Bitmap32 -> {
				writeHeader(Colorspace.RGBA)

				writeChunkCompressed("IDAT") {
					for (y in 0 until height) {
						write8(0) // no filter
						for (x in 0 until width) {
							val c = bitmap[x, y]
							write8(RGBA.getR(c))
							write8(RGBA.getG(c))
							write8(RGBA.getB(c))
							write8(RGBA.getA(c))
						}
					}
				}
			}
		}

		writeChunk("IEND") {
		}
	}

	override fun read(s: SyncStream): Bitmap {
		if (s.readS32_be() != MAGIC1) throw IllegalArgumentException("Invalid PNG file")
		s.readS32_be() // magic continuation

		var header = Header(0, 0, 0, Colorspace.GRAYSCALE, 0, 0, 0)
		val pngdata = ByteArrayOutputStream()
		var palette = IntArray(0x100) { -1 }

		fun SyncStream.readChunk() {
			val length = readS32_be()
			val type = readStringz(4)
			val data = readStream(length.toLong())
			val crc = readS32_be()

			when (type) {
				"IHDR" -> {
					header = data.run {
						Header(
							width = readS32_be(),
							height = readS32_be(),
							bits = readU8(),
							colorspace = Colorspace.BY_ID[readU8()] ?: Colorspace.RGBA,
							compressionmethod = readU8(),
							filtermethod = readU8(),
							interlacemethod = readU8()
						)
					}
				}
				"PLTE" -> {
					palette = Arrays.copyOf(palette, data.length.toInt() / 3)
					for (n in 0 until palette.size) {
						val r = data.readU8()
						val g = data.readU8()
						val b = data.readU8()
						val a = RGBA.getA(palette[n])
						palette[n] = RGBA.pack(r, g, b, a)
					}
				}
				"tRNS" -> {
					palette = Arrays.copyOf(palette, data.length.toInt())
					for (n in 0 until palette.size) {
						val r = RGBA.getR(palette[n])
						val g = RGBA.getG(palette[n])
						val b = RGBA.getB(palette[n])
						val a = data.readU8()
						palette[n] = RGBA.pack(r, g, b, a)
					}
				}
				"IDAT" -> {
					pngdata.write(data.readAll())
				}
				"IEND" -> {
				}
			}
			//println(type)
		}

		while (!s.eof) {
			s.readChunk()
		}

		val data = InflaterInputStream(ByteArrayInputStream(pngdata.toByteArray())).readBytes().openSync()

		var lastRow = UByteArray(header.width * header.bytes)
		var currentRow = UByteArray(header.width * header.bytes)

		when (header.bytes) {
			1 -> {
				val out = Bitmap8(header.width, header.height, palette = palette)
				for (y in 0 until header.height) {
					val filter = data.readU8()
					data.read(currentRow.data, 0, header.width * header.bytes)
					applyFilter(filter, lastRow, currentRow, header.bytes)
					out.setRow(y, currentRow.data)
					val temp = currentRow
					currentRow = lastRow
					lastRow = temp
				}
				return out
			}
			else -> {
				val row = IntArray(header.width)
				val out = Bitmap32(header.width, header.height)
				for (y in 0 until header.height) {
					val filter = data.readU8()
					data.read(currentRow.data, 0, header.width * header.bytes)
					applyFilter(filter, lastRow, currentRow, header.bytes)
					when (header.bytes) {
						3 -> {
							var m = 0
							for (n in 0 until header.width) {
								val r = currentRow[m++]
								val g = currentRow[m++]
								val b = currentRow[m++]
								row[n] = RGBA.packFast(r, g, b, 0xFF)
							}
						}
						4 -> {
							var m = 0
							for (n in 0 until header.width) {
								val r = currentRow[m++]
								val g = currentRow[m++]
								val b = currentRow[m++]
								val a = currentRow[m++]
								row[n] = RGBA.packFast(r, g, b, a)
								//System.out.printf("%02X,%02X,%02X,%02X\n", r, g, b, a)
							}
						}
						else -> TODO("Bytes: ${header.bytes}")
					}
					out.setRow(y, row)
					val temp = currentRow
					currentRow = lastRow
					lastRow = temp
				}

				return out
			}
		}
	}

	fun paethPredictor(a: Int, b: Int, c: Int): Int {
		val p = a + b - c
		val pa = Math.abs(p - a)
		val pb = Math.abs(p - b)
		val pc = Math.abs(p - c)
		return if ((pa <= pb) && (pa <= pc)) a else if (pb <= pc) b else c
	}

	fun applyFilter(filter: Int, p: UByteArray, c: UByteArray, bpp: Int) {
		when (filter) {
			0 -> Unit
			1 -> for (n in bpp until c.size) c[n] += c[n - bpp]
			2 -> for (n in 0 until c.size) c[n] += p[n]
			3 -> {
				for (n in 0 until bpp) c[n] += p[n] / 2
				for (n in bpp until c.size) c[n] += (c[n - bpp] + p[n]) / 2
			}
			4 -> {
				for (n in 0 until bpp) c[n] += p[n]
				for (n in bpp until c.size) c[n] += paethPredictor(c[n - bpp], p[n], p[n - bpp])
			}
			else -> TODO("Filter: $filter")
		}
	}
}