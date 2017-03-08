package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.YUVA
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readAll
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.util.*

class JPEG : ImageFormat("jpg", "jpeg") {
	override fun decodeHeader(s: SyncStream, filename: String): ImageInfo? = try {
		val decoder = JPEGDecoder(ByteArrayInputStream(s.readAll()))
		decoder.decodeHeader()
		ImageInfo().apply {
			this.width = decoder.imageWidth
			this.height = decoder.imageHeight
			this.bitsPerPixel = 24
		}
	} catch (e: Throwable) {
		null
	}

	override fun readImage(s: SyncStream, filename: String): ImageData {
		val decoder = JPEGDecoder(ByteArrayInputStream(s.readAll()))
		decoder.decodeHeader()
		decoder.startDecode()
		val out = Bitmap32(decoder.imageWidth, decoder.imageHeight)
		decoder.decodeRGB(out.data, 0, out.width, decoder.numMCURows)
		return ImageData(listOf(ImageFrame(out)))
	}

	/**
	 * A pure Java JPEG decoder
	 * <p>
	 * Partly based on code from Sean Barrett
	 *
	 * Converted, and optimized to Kotlin by Carlos Ballesteros
	 *
	 * @author Matthias Mann
	 */
	private class JPEGDecoder(private val iss: InputStream) {
		private val inputBuffer: ByteArray = ByteArray(0x4000)
		private var inputBufferPos: Int = 0
		private var inputBufferValid: Int = 0

		private var headerDecoded = false
		private var insideSOS = false
		private var foundEOI = false
		private var currentMCURow = 0

		private val idct2D = IDCT_2D()
		private val data = ShortArray(64)
		private val huffmanTables = Array(8) { Huffman.dummy }
		private val dequant = Array(4) { ByteArray(64) }

		private var components = arrayOf<Component>()
		private var order = arrayOf<Component>()

		private var codeBuffer = 0
		private var codeBits = 0
		private var marker = MARKER_NONE
		private var restartInterval = 0
		private var todo = 0
		var numMCUColumns: Int = 0
		var numMCURows: Int = 0
		var imageWidth: Int = 0; get () = ensureHeaderDecoded().run { field }
		var imageHeight: Int = 0; get () = ensureHeaderDecoded().run { field }
		var imgHMax: Int = 0
		var imgVMax: Int = 0
		var nomore: Boolean = false

		var decodeTmp: Array<ByteArray> = Array(3) { ByteArray(0) }
		var upsampleTmp: Array<ByteArray> = Array(3) { ByteArray(0) }

		fun decodeHeader() {
			if (headerDecoded) return
			headerDecoded = true
			var m = getMarker()
			if (m != 0xD8) throw IOException("no SOI")
			m = getMarker()
			while (m != 0xC0 && m != 0xC1) { // SOF
				processMarker(m)
				m = getMarker()
				while (m == MARKER_NONE) m = getMarker()
			}
			processSOF()
		}

		//fun getComponent(idx: Int): Component = ensureHeaderDecoded().run { components[idx] }
		//val numComponents: Int get() = ensureHeaderDecoded().run { components.size }
		//val mcuRowHeight: Int get() = ensureHeaderDecoded().run { imgVMax * 8 }

		fun startDecode(): Boolean {
			if (insideSOS) throw IllegalStateException("decode already started")
			if (foundEOI) return false

			decodeHeader()
			var m = getMarker()
			while (m != 0xD9) {  // EOI
				if (m == 0xDA) { // SOS
					processScanHeader()
					insideSOS = true
					currentMCURow = 0
					reset()
					return true
				} else {
					processMarker(m)
				}
				m = getMarker()
			}

			foundEOI = true
			return false
		}

		fun decodeRGB(dst: IntArray, outPos: Int, stride: Int, numMCURows: Int) {
			if (!insideSOS) throw IllegalStateException("decode not started")
			if (numMCURows <= 0 || currentMCURow + numMCURows > this.numMCURows) throw IllegalArgumentException("numMCURows")
			if (order.size != 3) throw UnsupportedOperationException("RGB decode only supported for 3 channels")

			val YUVstride = numMCUColumns * imgHMax * 8
			val requiresUpsampling = allocateDecodeTmp(YUVstride)

			val YtoRGB = if (order[0].upsampler != 0) upsampleTmp[0] else decodeTmp[0]
			val UtoRGB = if (order[1].upsampler != 0) upsampleTmp[1] else decodeTmp[1]
			val VtoRGB = if (order[2].upsampler != 0) upsampleTmp[2] else decodeTmp[2]

			for (j in 0 until numMCURows) {
				decodeMCUrow()
				if (requiresUpsampling) doUpsampling(YUVstride)
				var n = imgVMax * 8
				var opos = outPos
				n = Math.min(imageHeight - (currentMCURow - 1) * n, n)
				for (i in 0 until n) {
					YUVA.YUVtoRGB(dst, opos, YtoRGB, UtoRGB, VtoRGB, i * YUVstride, imageWidth)
					opos += stride
				}
				if (marker != MARKER_NONE) break
			}

			checkDecodeEnd()
		}

		//fun decodeRAW(buffer: Array<ByteBuffer>, strides: IntArray, numMCURows: Int) {
		//	if (!insideSOS) throw IllegalStateException("decode not started")
		//	if (numMCURows <= 0 || currentMCURow + numMCURows > this.numMCURows) throw IllegalArgumentException("numMCURows")
		//	val scanN = order.size
		//	if (scanN != components.size) throw UnsupportedOperationException("for RAW decode all components need to be decoded at once")
		//	if (scanN > buffer.size || scanN > strides.size) throw IllegalArgumentException("not enough buffers")
//
		//	for (compIdx in 0 until scanN) order[compIdx].outPos = buffer[compIdx].position()
//
		//	outer@ for (j in 0 until numMCURows) {
		//		++currentMCURow
		//		for (i in 0 until numMCUColumns) {
		//			for (compIdx in 0 until scanN) {
		//				val c = order[compIdx]
		//				val outStride = strides[compIdx]
		//				var outPosY = c.outPos + 8 * (i * c.blocksPerMCUHorz + j * c.blocksPerMCUVert * outStride)
//
		//				var y = 0
		//				while (y < c.blocksPerMCUVert) {
		//					var x = 0
		//					var outPos = outPosY
		//					while (x < c.blocksPerMCUHorz) {
		//						try {
		//							decodeBlock(data, c)
		//						} catch (ex: ArrayIndexOutOfBoundsException) {
		//							throwBadHuffmanCode()
		//						}
//
		//						idct2D.compute(buffer[compIdx], outPos, outStride, data)
		//						x++
		//						outPos += 8
		//					}
		//					y++
		//					outPosY += 8 * outStride
		//				}
		//			}
		//			if (--todo <= 0) {
		//				if (!checkRestart()) {
		//					break@outer
		//				}
		//			}
		//		}
		//	}
//
		//	checkDecodeEnd()
//
		//	for (compIdx in 0 until scanN) {
		//		val c = order[compIdx]
		//		buffer[compIdx].position(c.outPos + numMCURows * c.blocksPerMCUVert * 8 * strides[compIdx])
		//	}
		//}
//
		//fun decodeDCTCoeffs(buffer: Array<ShortBuffer>, numMCURows: Int) {
		//	if (!insideSOS) throw IllegalStateException("decode not started")
		//	if (numMCURows <= 0 || currentMCURow + numMCURows > this.numMCURows) throw IllegalArgumentException("numMCURows")
//
		//	val scanN = order.size
		//	if (scanN != components.size) throw UnsupportedOperationException("for RAW decode all components need to be decoded at once")
		//	if (scanN > buffer.size) throw IllegalArgumentException("not enough buffers")
		//	for (compIdx in 0 until scanN) order[compIdx].outPos = buffer[compIdx].position()
//
		//	try {
		//		outer@ for (j in 0 until numMCURows) {
		//			++currentMCURow
		//			for (i in 0 until numMCUColumns) {
		//				for (compIdx in 0 until scanN) {
		//					val c = order[compIdx]
		//					val sb = buffer[compIdx]
		//					val outStride = 64 * c.blocksPerMCUHorz * numMCUColumns
		//					var outPos = c.outPos + 64 * i * c.blocksPerMCUHorz + j * c.blocksPerMCUVert * outStride
//
		//					for (y in 0 until c.blocksPerMCUVert) {
		//						sb.position(outPos)
		//						for (x in 0 until c.blocksPerMCUHorz) {
		//							decodeBlock(data, c)
		//							sb.put(data)
		//						}
		//						outPos += outStride
		//					}
		//				}
		//				if (--todo <= 0 && !checkRestart()) break@outer
		//			}
		//		}
		//	} catch (ex: ArrayIndexOutOfBoundsException) {
		//		throwBadHuffmanCode()
		//	}
//
		//	checkDecodeEnd()
//
		//	for (compIdx in 0 until scanN) {
		//		val c = order[compIdx]
		//		val outStride = 64 * c.blocksPerMCUHorz * numMCUColumns
		//		buffer[compIdx].position(c.outPos + numMCURows * c.blocksPerMCUVert * outStride)
		//	}
		//}

		private fun checkDecodeEnd() {
			if (currentMCURow >= numMCURows || marker != MARKER_NONE) {
				insideSOS = false
				if (marker == MARKER_NONE) skipPadding()
			}
		}

		private fun fetch() {
			inputBufferPos = 0
			inputBufferValid = iss.read(inputBuffer)
			if (inputBufferValid <= 0) throw EOFException()
		}

		private fun read(buf: ByteArray, off: Int, len: Int) {
			var o = off
			var l = len
			while (l > 0) {
				val avail = inputBufferValid - inputBufferPos
				if (avail == 0) {
					fetch()
					continue
				}
				val copy = if (avail > l) l else avail
				System.arraycopy(inputBuffer, inputBufferPos, buf, o, copy)
				o += copy
				l -= copy
				inputBufferPos += copy
			}
		}

		private fun u8(): Int {
			if (inputBufferPos == inputBufferValid) fetch()
			return inputBuffer[inputBufferPos++].toInt() and 255
		}

		private fun u16(): Int = u8() shl 8 or u8()

		private fun skip(amount: Int) {
			var amnt = amount
			while (amnt > 0) {
				val inputBufferRemaining = inputBufferValid - inputBufferPos
				if (amnt > inputBufferRemaining) {
					amnt -= inputBufferRemaining
					fetch()
				} else {
					inputBufferPos += amnt
					return
				}
			}
		}

		private fun growBufferCheckMarker() {
			val c = u8()
			if (c != 0) {
				marker = c
				nomore = true
			}
		}

		private fun growBufferUnsafe() {
			do {
				var b = 0
				if (!nomore) {
					b = u8()
					if (b == 0xff) growBufferCheckMarker()
				}
				codeBuffer = codeBuffer or (b shl 24 - codeBits)
				codeBits += 8
			} while (codeBits <= 24)
		}

		private fun decode(h: Huffman): Int {
			if (codeBits < 16) growBufferUnsafe()
			val k = h.fast[codeBuffer.ushr(32 - Huffman.FAST_BITS)].toInt() and 255
			if (k < 0xFF) {
				val s = h.size[k].toInt()
				codeBuffer = codeBuffer shl s
				codeBits -= s
				return h.values[k].toInt() and 255
			}
			return decodeSlow(h)
		}

		private fun decodeSlow(h: Huffman): Int {
			val temp = codeBuffer.ushr(16)
			var s = Huffman.FAST_BITS + 1
			while (temp >= h.maxCode[s]) s++
			val k = temp.ushr(16 - s) + h.delta[s]
			codeBuffer = codeBuffer shl s
			codeBits -= s
			return h.values[k].toInt() and 255
		}

		private fun extendReceive(n: Int): Int {
			if (codeBits < 24) growBufferUnsafe()
			var k = codeBuffer.ushr(32 - n)
			codeBuffer = codeBuffer shl n
			codeBits -= n
			val limit = 1 shl n - 1
			if (k < limit) k -= limit * 2 - 1
			return k
		}

		private fun decodeBlock(data: ShortArray, c: Component) {
			Arrays.fill(data, 0.toShort())

			val dq = c.dequant

			run {
				val t = decode(c.huffDC)
				var dc = c.dcPred
				if (t > 0) {
					dc += extendReceive(t)
					c.dcPred = dc
				}

				data[0] = (dc * (dq[0].toInt() and 0xFF)).toShort()
			}

			val hac = c.huffAC

			var k = 1
			do {
				val rs = decode(hac)
				k += rs shr 4
				val s = rs and 15
				if (s != 0) {
					val v = extendReceive(s) * (dq[k].toInt() and 0xFF)
					data[dezigzag[k].toInt()] = v.toShort()
				} else if (rs != 0xF0) {
					break
				}
			} while (++k < 64)
		}

		private fun getMarker(): Int {
			var m = marker
			if (m != MARKER_NONE) {
				marker = MARKER_NONE
				return m
			}
			m = u8()
			if (m != 0xFF) return MARKER_NONE
			do {
				m = u8()
			} while (m == 0xFF)
			return m
		}

		private fun reset() {
			codeBits = 0
			codeBuffer = 0
			nomore = false
			marker = MARKER_NONE
			todo = if (restartInterval != 0) restartInterval else Integer.MAX_VALUE
			for (c in components) c.dcPred = 0
		}

		private fun checkRestart(): Boolean {
			if (codeBits < 24) growBufferUnsafe()
			if (marker >= 0xD0 && marker <= 0xD7) {
				reset()
				return true
			}
			return false
		}

		private fun processMarker(marker: Int) {
			if (marker >= 0xE0 && (marker <= 0xEF || marker == 0xFE)) {
				val l = u16() - 2
				if (l < 0) throw IOException("bad length")
				skip(l)
				return
			}

			when (marker) {
				MARKER_NONE -> throw IOException("Expected marker")
				0xC2 -> throw IOException("Progressive JPEG not supported") // SOF - progressive
				0xDD -> { // DRI - specify restart interval
					if (u16() != 4) throw IOException("bad DRI length")
					restartInterval = u16()
				}
				0xDB -> {// DQT - define dequant table
					var l = u16() - 2
					while (l >= 65) {
						val q = u8()
						val p = q shr 4
						val t = q and 15
						if (p != 0) throw IOException("bad DQT type")
						if (t > 3) throw IOException("bad DQT table")
						read(dequant[t], 0, 64)
						l -= 65
					}
					if (l != 0) throw IOException("bad DQT length")
				}
				0xC4 -> { // DHT - define huffman table
					var l = u16() - 2
					while (l > 17) {
						val q = u8()
						val tc = q shr 4
						val th = q and 15
						if (tc > 1 || th > 3) throw IOException("bad DHT header")
						val tmp = idct2D.tmp2D   // reuse memory
						for (i in 0 until 16) tmp[i] = u8()
						val h = Huffman(tmp)
						val m = h.numSymbols
						l -= 17 + m
						if (l < 0) throw IOException("bad DHT length")
						read(h.values, 0, m)
						huffmanTables[tc * 4 + th] = h
					}
					if (l != 0) throw IOException("bad DHT length")
				}

				else -> throw IOException("Unknown marker: " + Integer.toHexString(marker))
			}
		}

		private fun skipPadding() {
			var x: Int
			do {
				x = u8()
			} while (x == 0)
			if (x == 0xFF) marker = u8()
		}

		private fun processScanHeader() {
			val ls = u16()
			val scanN = u8()
			if (scanN < 1 || scanN > 4) throw IOException("bad SOS component count")
			if (ls != 6 + 2 * scanN) throw IOException("bad SOS length")
			val lorder = arrayOfNulls<Component>(scanN)
			for (i in 0 until scanN) {
				val id = u8()
				val q = u8()
				for (c in components) {
					if (c.id == id) {
						val hd = q shr 4
						val ha = q and 15
						if (hd > 3 || ha > 3) throw IOException("bad huffman table index")
						c.huffDC = huffmanTables[hd]
						c.huffAC = huffmanTables[ha + 4]
						if (c.huffDC.dummy || c.huffAC.dummy) throw IOException("bad huffman table index")
						lorder[i] = c
						break
					}
				}
				if (lorder[i] == null) throw IOException("unknown color component")
			}
			order = lorder.requireNoNulls()

			if (u8() != 0) throw IOException("bad SOS")
			u8()
			if (u8() != 0) throw IOException("bad SOS")
		}

		private fun processSOF() {
			val lf = u16()
			if (lf < 11) throw IOException("bad SOF length")
			if (u8() != 8) throw IOException("only 8 bit JPEG supported")
			imageHeight = u16()
			imageWidth = u16()
			if (imageWidth <= 0 || imageHeight <= 0) throw IOException("Invalid image size")
			val numComps = u8()
			if (numComps != 3 && numComps != 1) throw IOException("bad component count")
			if (lf != 8 + 3 * numComps) throw IOException("bad SOF length")

			var hMax = 1
			var vMax = 1

			components = (0 until numComps).map {
				val c = Component(u8()).apply {}
				val q = u8()
				val tq = u8()

				c.blocksPerMCUHorz = q shr 4
				c.blocksPerMCUVert = q and 15

				if (c.blocksPerMCUHorz == 0 || c.blocksPerMCUHorz > 4) throw IOException("bad H")
				if (c.blocksPerMCUVert == 0 || c.blocksPerMCUVert > 4) throw IOException("bad V")
				if (tq > 3) throw IOException("bad TQ")
				c.dequant = dequant[tq]

				hMax = Math.max(hMax, c.blocksPerMCUHorz)
				vMax = Math.max(vMax, c.blocksPerMCUVert)
				c
			}.toTypedArray()

			val mcuW = hMax * 8
			val mcuH = vMax * 8

			imgHMax = hMax
			imgVMax = vMax
			numMCUColumns = (imageWidth + mcuW - 1) / mcuW
			numMCURows = (imageHeight + mcuH - 1) / mcuH

			for (i in 0 until numComps) {
				val c = components[i]
				c.width = (imageWidth * c.blocksPerMCUHorz + hMax - 1) / hMax
				c.height = (imageHeight * c.blocksPerMCUVert + vMax - 1) / vMax
				c.minReqWidth = numMCUColumns * c.blocksPerMCUHorz * 8
				c.minReqHeight = numMCURows * c.blocksPerMCUVert * 8

				if (c.blocksPerMCUHorz < hMax) c.upsampler = c.upsampler or 1
				if (c.blocksPerMCUVert < vMax) c.upsampler = c.upsampler or 2
			}
		}

		private fun ensureHeaderDecoded() {
			if (!headerDecoded) throw IllegalStateException("need to decode header first")
		}

		private fun allocateDecodeTmp(YUVstride: Int): Boolean {
			var requiresUpsampling = false
			for (compIdx in 0 until 3) {
				val c = order[compIdx]
				val reqSize = c.minReqWidth * c.blocksPerMCUVert * 8
				if (decodeTmp[compIdx].size < reqSize) decodeTmp[compIdx] = ByteArray(reqSize)
				if (c.upsampler != 0) {
					val upsampleReq = imgVMax * 8 * YUVstride
					if (upsampleTmp[compIdx].size < upsampleReq) {
						upsampleTmp[compIdx] = ByteArray(upsampleReq)
					}
					requiresUpsampling = true
				}
			}
			return requiresUpsampling
		}

		private fun decodeMCUrow() {
			++currentMCURow
			for (i in 0 until numMCUColumns) {
				for (compIdx in 0 until 3) {
					val c = order[compIdx]
					val outStride = c.minReqWidth
					var outPosY = 8 * i * c.blocksPerMCUHorz

					var y = 0
					while (y < c.blocksPerMCUVert) {
						var x = 0
						var outPos = outPosY
						while (x < c.blocksPerMCUHorz) {
							decodeBlock(data, c)

							idct2D.compute(decodeTmp[compIdx], outPos, outStride, data)
							x++
							outPos += 8
						}
						y++
						outPosY += 8 * outStride
					}
				}
				if (--todo <= 0 && !checkRestart()) break
			}
		}

		private fun doUpsampling(YUVstride: Int) {
			for (compIdx in 0 until 3) {
				val c = order[compIdx]
				val inStride = c.minReqWidth
				val height = c.blocksPerMCUVert * 8
				when (c.upsampler) {
					1 -> for (i in 0 until height) {
						upsampleH2(upsampleTmp[compIdx], i * YUVstride, decodeTmp[compIdx], i * inStride, c.width)
					}

					2 -> {
						run {
							var i = 0
							var inPos0 = 0
							var inPos1 = 0
							while (i < height) {
								upsampleV2(upsampleTmp[compIdx], (i * 2 + 0) * YUVstride, decodeTmp[compIdx], inPos0, inPos1, c.width)
								upsampleV2(upsampleTmp[compIdx], (i * 2 + 1) * YUVstride, decodeTmp[compIdx], inPos1, inPos0, c.width)
								inPos0 = inPos1
								inPos1 += inStride
								i++
							}
						}
						var i = 0
						var inPos0 = 0
						var inPos1 = 0
						while (i < height) {
							upsampleHV2(upsampleTmp[compIdx], (i * 2 + 0) * YUVstride, decodeTmp[compIdx], inPos0, inPos1, c.width)
							upsampleHV2(upsampleTmp[compIdx], (i * 2 + 1) * YUVstride, decodeTmp[compIdx], inPos1, inPos0, c.width)
							inPos0 = inPos1
							inPos1 += inStride
							i++
						}
					}

					3 -> {
						var i = 0
						var inPos0 = 0
						var inPos1 = 0
						while (i < height) {
							upsampleHV2(upsampleTmp[compIdx], (i * 2 + 0) * YUVstride, decodeTmp[compIdx], inPos0, inPos1, c.width)
							upsampleHV2(upsampleTmp[compIdx], (i * 2 + 1) * YUVstride, decodeTmp[compIdx], inPos1, inPos0, c.width)
							inPos0 = inPos1
							inPos1 += inStride
							i++
						}
					}
				}
			}
		}

		class Huffman(val count: IntArray, val dummy: Boolean = false) {
			companion object {
				internal val FAST_BITS = 9
				//internal val FAST_MASK = (1 shl FAST_BITS) - 1
				val dummy = Huffman(IntArray(16), dummy = true)
			}

			val numSymbols = (0 until 16).sumBy { count[it] }
			internal val fast = ByteArray(1 shl FAST_BITS)
			internal val values = ByteArray(numSymbols)
			internal val size = ByteArray(numSymbols).apply {
				var k = 0
				for (i in 0 until 16) {
					for (j in 0 until count[i]) {
						this[k++] = (i + 1).toByte()
					}
				}
			}
			internal val maxCode = IntArray(18)
			internal val delta = IntArray(17)

			init {
				val code = IntArray(256)

				var k = 0
				run {
					var c = 0
					for (i in 1 .. 16) {
						delta[i] = k - c
						if (k < numSymbols && size[k].toInt() == i) {
							do {
								code[k++] = c++
							} while (k < numSymbols && size[k].toInt() == i)
							if (c - 1 >= 1 shl i) throw IOException("Bad code length")
						}
						maxCode[i] = c shl 16 - i
						c = c shl 1
					}
				}
				maxCode[17] = Integer.MAX_VALUE

				Arrays.fill(fast, (-1).toByte())
				run {
					for (i in 0 until k) {
						val s = size[i].toInt()
						if (s <= FAST_BITS) {
							val c = code[i] shl FAST_BITS - s
							val m = 1 shl FAST_BITS - s
							for (j in 0 until m) fast[c + j] = i.toByte()
						}
					}
				}
			}
		}

		class IDCT_2D {
			internal val tmp2D = IntArray(64)

			private fun computeV(data: ShortArray) {
				val tmp = tmp2D

				var i = 0
				do {
					computeVOne(data, i, tmp)
				} while (++i < 8)
			}

			private fun computeVOne(data: ShortArray, i: Int, tmp: IntArray) {
				val s0 = data[i + 0].toInt()
				val s1 = data[i + 8].toInt()
				val s2 = data[i + 16].toInt()
				val s3 = data[i + 24].toInt()
				val s4 = data[i + 32].toInt()
				val s5 = data[i + 40].toInt()
				val s6 = data[i + 48].toInt()
				val s7 = data[i + 56].toInt()

				val p1 = (s2 + s6) * C00
				val p2 = (s0 + s4 shl 12) + 512
				val p3 = (s0 - s4 shl 12) + 512
				val p4 = p1 + s6 * C01
				val p5 = p1 + s2 * C02

				val x0 = p2 + p5
				val x3 = p2 - p5
				val x1 = p3 + p4
				val x2 = p3 - p4

				val p1b = s7 + s1
				val p2b = s5 + s3
				val p3b = s7 + s3
				val p4b = s5 + s1
				val p5b = (p3b + p4b) * C03

				val p1c = p5b + p1b * C08
				val p2c = p5b + p2b * C09
				val p3c = p3b * C10
				val p4c = p4b * C11

				val t0 = s7 * C04 + p1c + p3c
				val t1 = s5 * C05 + p2c + p4c
				val t2 = s3 * C06 + p2c + p3c
				val t3 = s1 * C07 + p1c + p4c

				tmp[i + 0] = x0 + t3 shr 10
				tmp[i + 56] = x0 - t3 shr 10
				tmp[i + 8] = x1 + t2 shr 10
				tmp[i + 48] = x1 - t2 shr 10
				tmp[i + 16] = x2 + t1 shr 10
				tmp[i + 40] = x2 - t1 shr 10
				tmp[i + 24] = x3 + t0 shr 10
				tmp[i + 32] = x3 - t0 shr 10
			}
//
			//fun compute(out: ByteBuffer, outPos: Int, outStride: Int, data: ShortArray) {
			//	var opos = outPos
			//	computeV(data)
//
			//	val tmp = tmp2D
			//	var i = 0
			//	while (i < 64) {
			//		val s0 = tmp[i + 0] + (257 shl 4)
			//		val s1 = tmp[i + 1]
			//		val s2 = tmp[i + 2]
			//		val s3 = tmp[i + 3]
			//		val s4 = tmp[i + 4]
			//		val s5 = tmp[i + 5]
			//		val s6 = tmp[i + 6]
			//		val s7 = tmp[i + 7]
//
			//		var p1 = (s2 + s6) * C0
			//		var p2 = s0 + s4 shl 12
			//		var p3 = s0 - s4 shl 12
			//		var p4 = p1 + s6 * C1
			//		var p5 = p1 + s2 * C2
//
			//		val x0 = p2 + p5
			//		val x3 = p2 - p5
			//		val x1 = p3 + p4
			//		val x2 = p3 - p4
//
			//		p1 = s7 + s1
			//		p2 = s5 + s3
			//		p3 = s7 + s3
			//		p4 = s5 + s1
			//		p5 = (p3 + p4) * C3
//
			//		p1 = p5 + p1 * C8
			//		p2 = p5 + p2 * C9
			//		p3 = p3 * C10
			//		p4 = p4 * C11
//
			//		val t0 = s7 * C4 + p1 + p3
			//		val t1 = s5 * C5 + p2 + p4
			//		val t2 = s3 * C6 + p2 + p3
			//		val t3 = s1 * C7 + p1 + p4
//
			//		out.put(opos + 0, clampShift17(x0 + t3))
			//		out.put(opos + 7, clampShift17(x0 - t3))
			//		out.put(opos + 1, clampShift17(x1 + t2))
			//		out.put(opos + 6, clampShift17(x1 - t2))
			//		out.put(opos + 2, clampShift17(x2 + t1))
			//		out.put(opos + 5, clampShift17(x2 - t1))
			//		out.put(opos + 3, clampShift17(x3 + t0))
			//		out.put(opos + 4, clampShift17(x3 - t0))
//
			//		opos += outStride
			//		i += 8
			//	}
			//}

			fun compute(out: ByteArray, outPos: Int, outStride: Int, data: ShortArray) {
				var opos = outPos
				computeV(data)
				val tmp = tmp2D
				var i = 0
				while (i < 64) {
					computeOne(i, opos, out, tmp)
					opos += outStride
					i += 8
				}
			}

			private fun computeOne(i: Int, opos: Int, out: ByteArray, tmp: IntArray) {
				val s0 = tmp[i + 0] + (257 shl 4)
				val s1 = tmp[i + 1]
				val s2 = tmp[i + 2]
				val s3 = tmp[i + 3]
				val s4 = tmp[i + 4]
				val s5 = tmp[i + 5]
				val s6 = tmp[i + 6]
				val s7 = tmp[i + 7]

				val p1 = (s2 + s6) * C00
				val p2 = s0 + s4 shl 12
				val p3 = s0 - s4 shl 12
				val p4 = p1 + s6 * C01
				val p5 = p1 + s2 * C02

				val x0 = p2 + p5
				val x3 = p2 - p5
				val x1 = p3 + p4
				val x2 = p3 - p4

				val p1b = s7 + s1
				val p2b = s5 + s3
				val p3b = s7 + s3
				val p4b = s5 + s1
				val p5b = (p3b + p4b) * C03

				val p1c = p5b + p1b * C08
				val p2c = p5b + p2b * C09
				val p3c = p3b * C10
				val p4c = p4b * C11

				val t0 = s7 * C04 + p1c + p3c
				val t1 = s5 * C05 + p2c + p4c
				val t2 = s3 * C06 + p2c + p3c
				val t3 = s1 * C07 + p1c + p4c

				out[opos + 0] = clampShift17(x0 + t3)
				out[opos + 7] = clampShift17(x0 - t3)
				out[opos + 1] = clampShift17(x1 + t2)
				out[opos + 6] = clampShift17(x1 - t2)
				out[opos + 2] = clampShift17(x2 + t1)
				out[opos + 5] = clampShift17(x2 - t1)
				out[opos + 3] = clampShift17(x3 + t0)
				out[opos + 4] = clampShift17(x3 - t0)
			}

			companion object {
				//@Strictfp private fun f2f(x: Double): Int = Math.round(Math.scalb(x, 12)).toInt()
				//private val C0 = f2f(0.541196100)
				//private val C1 = f2f(-1.847759065)
				//private val C2 = f2f(0.765366865)
				//private val C3 = f2f(1.175875602)
				//private val C4 = f2f(0.298631336)
				//private val C5 = f2f(2.053119869)
				//private val C6 = f2f(3.072711026)
				//private val C7 = f2f(1.501321110)
				//private val C8 = f2f(-0.899976223)
				//private val C9 = f2f(-2.562915447)
				//private val C10 = f2f(-1.961570560)
				//private val C11 = f2f(-0.390180644)

				const val C00 = 2217
				const val C01 = -7568
				const val C02 = 3135
				const val C03 = 4816
				const val C04 = 1223
				const val C05 = 8410
				const val C06 = 12586
				const val C07 = 6149
				const val C08 = -3686
				const val C09 = -10498
				const val C10 = -8035
				const val C11 = -1598

				private fun clampShift17(x: Int): Byte = (if (x < 0) 0 else if (x > 255 shl 17) 255 else x.ushr(17)).toByte()
			}
		}

		class Component(val id: Int) {
			var dcPred = 0
			var huffDC = Huffman.dummy
			var huffAC = Huffman.dummy
			var dequant = ByteArray(0)
			var blocksPerMCUVert = 0
			var blocksPerMCUHorz = 0
			var width = 0
			var height = 0
			var minReqWidth = 0
			var minReqHeight = 0
			//var outPos = 0
			var upsampler = 0
		}

		companion object {
			internal val MARKER_NONE = 0xFF

			//private fun throwBadHuffmanCode(): Nothing = throw IOException("Bad huffman code")

			private fun upsampleH2(out: ByteArray, outPos: Int, inp: ByteArray, inPos: Int, width: Int) {
				if (width == 1) {
					out[outPos + 1] = inp[inPos]
					out[outPos] = out[outPos + 1]
				} else {
					var i0 = inp[inPos].toInt() and 255
					var i1 = inp[inPos + 1].toInt() and 255
					out[outPos] = i0.toByte()
					out[outPos + 1] = (i0 * 3 + i1 + 2 shr 2).toByte()
					for (i in 2 until width) {
						val i2 = inp[inPos + i].toInt() and 255
						val n = i1 * 3 + 2
						out[outPos + i * 2 - 2] = (n + i0 shr 2).toByte()
						out[outPos + i * 2 - 1] = (n + i2 shr 2).toByte()
						i0 = i1
						i1 = i2
					}
					out[outPos + width * 2 - 2] = (i0 * 3 + i1 + 2 shr 2).toByte()
					out[outPos + width * 2 - 1] = i1.toByte()
				}
			}

			private fun upsampleV2(out: ByteArray, outPos: Int, inp: ByteArray, inPos0: Int, inPos1: Int, width: Int) {
				for (i in 0 until width) out[outPos + i] = (3 * (inp[inPos0 + i].toInt() and 255) + (inp[inPos1 + i].toInt() and 255) + 2 shr 2).toByte()
			}

			private fun upsampleHV2(out: ByteArray, outPos: Int, inp: ByteArray, inPos0: Int, inPos1: Int, width: Int) {
				if (width == 1) {
					val i0 = inp[inPos0].toInt() and 255
					val i1 = inp[inPos1].toInt() and 255
					out[outPos + 1] = (i0 * 3 + i1 + 2 shr 2).toByte()
					out[outPos] = out[outPos + 1]
				} else {
					var i1 = 3 * (inp[inPos0].toInt() and 255) + (inp[inPos1].toInt() and 255)
					out[outPos] = (i1 + 2 shr 2).toByte()
					for (i in 1 until width) {
						val i0 = i1
						i1 = 3 * (inp[inPos0 + i].toInt() and 255) + (inp[inPos1 + i].toInt() and 255)
						out[outPos + i * 2 - 1] = (3 * i0 + i1 + 8 shr 4).toByte()
						out[outPos + i * 2] = (3 * i1 + i0 + 8 shr 4).toByte()
					}
					out[outPos + width * 2 - 1] = (i1 + 2 shr 2).toByte()
				}
			}

			internal val dezigzag = ("" +
				"\u0000\u0001\u0008\u0010\u0009\u0002\u0003\u000a" +
				"\u0011\u0018\u0020\u0019\u0012\u000b\u0004\u0005" +
				"\u000c\u0013\u001a\u0021\u0028\u0030\u0029\u0022" +
				"\u001b\u0014\u000d\u0006\u0007\u000e\u0015\u001c" +
				"\u0023\u002a\u0031\u0038\u0039\u0032\u002b\u0024" +
				"\u001d\u0016\u000f\u0017\u001e\u0025\u002c\u0033" +
				"\u003a\u003b\u0034\u002d\u0026\u001f\u0027\u002e" +
				"\u0035\u003c\u003d\u0036\u002f\u0037\u003e\u003f" +
				"\u003f\u003f\u003f\u003f\u003f\u003f\u003f\u003f" +
				"\u003f\u003f\u003f\u003f\u003f\u003f\u003f"
				).toCharArray()
		}
	}
}
