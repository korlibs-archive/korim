package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmap8
import com.soywiz.korim.color.RGB
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.UByteArray
import com.soywiz.korio.util.toIntSafe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterInputStream
import java.util.zip.InflaterInputStream

class PNG : ImageFormat() {
	companion object {
		const val MAGIC1 = 0x89504E47.toInt()
		const val MAGIC2 = 0x0D0A1A0A.toInt()
	}

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

	override fun decodeHeader(s: SyncStream, filename: String): ImageInfo? = try {
		val header = readCommon(s, readHeader = true) as Header
		ImageInfo().apply {
			this.width = header.width
			this.height = header.height
			this.bitsPerPixel = header.bitsPerChannel
		}
	} catch (t: Throwable) {
		null
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
			return writeChunk(name, MemorySyncStreamToByteArray { callback() })
		}

		fun writeChunkCompressed(name: String, callback: SyncStream.() -> Unit) {
			return writeChunk(name, compress(MemorySyncStreamToByteArray { callback() }))
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

	private fun readCommon(s: SyncStream, readHeader: Boolean): Any? {
		if (s.readS32_be() != MAGIC1) throw IllegalArgumentException("Invalid PNG file")
		s.readS32_be() // magic continuation

		var pheader: Header? = null
		val pngdata = ByteArrayOutputStream(s.length.toIntSafe())
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
				}
				"PLTE" -> {
					paletteCount = Math.max(paletteCount, data.length.toInt() / 3)
					data.read(rgbPalette.data, 0, data.length.toInt())
				}
				"tRNS" -> {
					paletteCount = Math.max(paletteCount, data.length.toInt())
					data.read(aPalette.data, 0, data.length.toInt())
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
			if (readHeader && pheader != null) return pheader
		}

		val header = pheader ?: throw IllegalArgumentException("PNG without header!")

		val estimatedBytes = (1 + header.width) * header.height * header.bytes
		val stride = header.stride

		val data = InflaterInputStream(ByteArrayInputStream(pngdata.toByteArray())).readBytes(estimatedBytes).openSync()

		var lastRow = UByteArray(stride)
		var currentRow = UByteArray(stride)

		when (header.bytes) {
			1 -> {
				val palette = (0 until paletteCount).map { RGBA(rgbPalette[it * 3 + 0], rgbPalette[it * 3 + 1], rgbPalette[it * 3 + 2], aPalette[it]) }.toIntArray()
				val out = Bitmap8(header.width, header.height, palette = palette)
				for (y in 0 until header.height) {
					val filter = data.readU8()
					data.read(currentRow.data, 0, stride)
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
					data.read(currentRow.data, 0, stride)
					applyFilter(filter, lastRow, currentRow, header.bytes)
					when (header.bytes) {
						3 -> RGB.decode(currentRow.data, 0, row, 0, header.width)
						4 -> RGBA.decode(currentRow.data, 0, row, 0, header.width)
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

	override fun readFrames(s: SyncStream, filename: String): List<ImageFrame> {
		return listOf(ImageFrame(readCommon(s, readHeader = false) as Bitmap))
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