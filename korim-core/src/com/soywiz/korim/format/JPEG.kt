package com.soywiz.korim.format

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.util.UByteArray
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.*

object JPEG : ImageFormat() {
	const val MAGIC = 0xFFD8

	override fun decodeHeader(s: SyncStream): ImageInfo? = try {
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

	override fun readFrames(s: SyncStream): List<ImageFrame> {
		val decoder = JPEGDecoder(ByteArrayInputStream(s.readAll()))
		decoder.decodeHeader()
		val width = decoder.imageWidth
		val height = decoder.imageHeight
		//val format = Texture.Format.RGBA;
		decoder.startDecode();
		val data = ByteArray(width * height * 4)
		val udata = UByteArray(data)
		val bb = ByteBuffer.wrap(data)
		decoder.decodeRGB(bb, width * 4, decoder.numMCURows)
		val out = Bitmap32(width, height)
		var n = 0
		for (y in 0 until height) {
			for (x in 0 until width) {
				val r = udata[n++]
				val g = udata[n++]
				val b = udata[n++]
				val a = udata[n++]
				out[x, y] = RGBA.packFast(r, g, b, a)
			}
		}
		return listOf(ImageFrame(out))
	}

	override fun write(bitmap: Bitmap, s: SyncStream) {
		super.write(bitmap, s)
	}
}

/**
 * A pure Java JPEG decoder
 * <p>
 * Partly based on code from Sean Barrett
 *
 * Converted to Kotlin by Carlos Ballesteros
 *
 * @author Matthias Mann
 */
class JPEGDecoder
(private val `is`: InputStream) {
	private val inputBuffer: ByteArray
	private var inputBufferPos: Int = 0
	private var inputBufferValid: Int = 0

	var isIgnoreIOerror: Boolean = false
		set(ignoreIOerror) {
			if (headerDecoded) throw IllegalStateException("header already decoded")
			field = ignoreIOerror
		}

	private var headerDecoded: Boolean = false
	private var insideSOS: Boolean = false
	private var foundEOI: Boolean = false
	private var currentMCURow: Int = 0

	private val idct2D: IDCT_2D
	private val data: ShortArray
	private val huffmanTables: Array<Huffman?>
	private val dequant: Array<ByteArray>

	private var components: Array<Component?>? = null
	private var order: Array<Component?>? = null

	private var codeBuffer: Int = 0
	private var codeBits: Int = 0
	private var marker = MARKER_NONE
	private var restartInterval: Int = 0
	private var todo: Int = 0
	var numMCUColumns: Int = 0
	var numMCURows: Int = 0
	var imageWidth: Int = 0; get () = ensureHeaderDecoded().run { field }
	var imageHeight: Int = 0; get () = ensureHeaderDecoded().run { field }
	var imgHMax: Int = 0
	var imgVMax: Int = 0
	var nomore: Boolean = false

	var decodeTmp: Array<ByteArray?>? = null
	var upsampleTmp: Array<ByteArray?>? = null

	init {
		this.inputBuffer = ByteArray(4096)

		this.idct2D = IDCT_2D()
		this.data = ShortArray(64)
		this.huffmanTables = arrayOfNulls<Huffman>(8)
		this.dequant = Array(4) { ByteArray(64) }
	}

	@Throws(IOException::class)
	fun decodeHeader() {
		if (!headerDecoded) {
			headerDecoded = true

			var m = getMarker()
			if (m != 0xD8) {
				throw IOException("no SOI")
			}
			m = getMarker()
			while (m != 0xC0 && m != 0xC1) { // SOF
				processMarker(m)
				m = getMarker()
				while (m == MARKER_NONE) {
					m = getMarker()
				}
			}

			processSOF()
		}
	}

	val numComponents: Int
		get() {
			ensureHeaderDecoded()
			return components!!.size
		}

	fun getComponent(idx: Int): Component {
		ensureHeaderDecoded()
		return components!![idx]!!
	}

	val mcuRowHeight: Int
		get() {
			ensureHeaderDecoded()
			return imgVMax * 8
		}

	@Throws(IOException::class)
	fun startDecode(): Boolean {
		if (insideSOS) {
			throw IllegalStateException("decode already started")
		}
		if (foundEOI) {
			return false
		}

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

	@Throws(IOException::class)
	fun decodeRGB(dst: ByteBuffer, stride: Int, numMCURows: Int) {
		if (!insideSOS) {
			throw IllegalStateException("decode not started")
		}

		if (numMCURows <= 0 || currentMCURow + numMCURows > this.numMCURows) {
			throw IllegalArgumentException("numMCURows")
		}

		if (order!!.size != 3) {
			throw UnsupportedOperationException("RGB decode only supported for 3 channels")
		}

		val YUVstride = numMCUColumns * imgHMax * 8
		val requiresUpsampling = allocateDecodeTmp(YUVstride)

		val YtoRGB = if (order!![0]!!.upsampler != 0) upsampleTmp!![0]!! else decodeTmp!![0]!!
		val UtoRGB = if (order!![1]!!.upsampler != 0) upsampleTmp!![1]!! else decodeTmp!![1]!!
		val VtoRGB = if (order!![2]!!.upsampler != 0) upsampleTmp!![2]!! else decodeTmp!![2]!!

		for (j in 0..numMCURows - 1) {
			decodeMCUrow()

			if (requiresUpsampling) {
				doUpsampling(YUVstride)
			}

			var outPos = dst.position()
			var n = imgVMax * 8
			n = Math.min(imageHeight - (currentMCURow - 1) * n, n)
			for (i in 0..n - 1) {
				YUVtoRGB(dst, outPos, YtoRGB, UtoRGB, VtoRGB, i * YUVstride, imageWidth)
				outPos += stride
			}
			dst.position(outPos)

			if (marker != MARKER_NONE) {
				break
			}
		}

		checkDecodeEnd()
	}

	@Throws(IOException::class)
	fun decodeRAW(buffer: Array<ByteBuffer>, strides: IntArray, numMCURows: Int) {
		if (!insideSOS) {
			throw IllegalStateException("decode not started")
		}

		if (numMCURows <= 0 || currentMCURow + numMCURows > this.numMCURows) {
			throw IllegalArgumentException("numMCURows")
		}

		val scanN = order!!.size
		if (scanN != components!!.size) {
			throw UnsupportedOperationException("for RAW decode all components need to be decoded at once")
		}
		if (scanN > buffer.size || scanN > strides.size) {
			throw IllegalArgumentException("not enough buffers")
		}

		for (compIdx in 0..scanN - 1) {
			order!![compIdx]!!.outPos = buffer[compIdx].position()
		}

		outer@ for (j in 0..numMCURows - 1) {
			++currentMCURow
			for (i in 0..numMCUColumns - 1) {
				for (compIdx in 0..scanN - 1) {
					val c = order!![compIdx]!!
					val outStride = strides[compIdx]
					var outPosY = c.outPos + 8 * (i * c.blocksPerMCUHorz + j * c.blocksPerMCUVert * outStride)

					var y = 0
					while (y < c.blocksPerMCUVert) {
						var x = 0
						var outPos = outPosY
						while (x < c.blocksPerMCUHorz) {
							try {
								decodeBlock(data, c)
							} catch (ex: ArrayIndexOutOfBoundsException) {
								throwBadHuffmanCode()
							}

							idct2D.compute(buffer[compIdx], outPos, outStride, data)
							x++
							outPos += 8
						}
						y++
						outPosY += 8 * outStride
					}
				}
				if (--todo <= 0) {
					if (!checkRestart()) {
						break@outer
					}
				}
			}
		}

		checkDecodeEnd()

		for (compIdx in 0..scanN - 1) {
			val c = order!![compIdx]!!
			buffer[compIdx].position(c.outPos + numMCURows * c.blocksPerMCUVert * 8 * strides[compIdx])
		}
	}

	@Throws(IOException::class)
	fun decodeDCTCoeffs(buffer: Array<ShortBuffer>, numMCURows: Int) {
		if (!insideSOS) {
			throw IllegalStateException("decode not started")
		}

		if (numMCURows <= 0 || currentMCURow + numMCURows > this.numMCURows) {
			throw IllegalArgumentException("numMCURows")
		}

		val scanN = order!!.size
		if (scanN != components!!.size) {
			throw UnsupportedOperationException("for RAW decode all components need to be decoded at once")
		}
		if (scanN > buffer.size) {
			throw IllegalArgumentException("not enough buffers")
		}

		for (compIdx in 0..scanN - 1) {
			order!![compIdx]!!.outPos = buffer[compIdx].position()
		}

		outer@ for (j in 0..numMCURows - 1) {
			++currentMCURow
			for (i in 0..numMCUColumns - 1) {
				for (compIdx in 0..scanN - 1) {
					val c = order!![compIdx]!!
					val sb = buffer[compIdx]
					val outStride = 64 * c.blocksPerMCUHorz * numMCUColumns
					var outPos = c.outPos + 64 * i * c.blocksPerMCUHorz + j * c.blocksPerMCUVert * outStride

					for (y in 0..c.blocksPerMCUVert - 1) {
						sb.position(outPos)
						for (x in 0..c.blocksPerMCUHorz - 1) {
							try {
								decodeBlock(data, c)
							} catch (ex: ArrayIndexOutOfBoundsException) {
								throwBadHuffmanCode()
							}

							sb.put(data)
						}
						outPos += outStride
					}
				}
				if (--todo <= 0) {
					if (!checkRestart()) {
						break@outer
					}
				}
			}
		}

		checkDecodeEnd()

		for (compIdx in 0..scanN - 1) {
			val c = order!![compIdx]!!
			val outStride = 64 * c.blocksPerMCUHorz * numMCUColumns
			buffer[compIdx].position(c.outPos + numMCURows * c.blocksPerMCUVert * outStride)
		}
	}

	@Throws(IOException::class)
	private fun checkDecodeEnd() {
		if (currentMCURow >= numMCURows || marker != MARKER_NONE) {
			insideSOS = false
			if (marker == MARKER_NONE) {
				skipPadding()
			}
		}
	}

	@Throws(IOException::class)
	private fun fetch() {
		try {
			inputBufferPos = 0
			inputBufferValid = `is`.read(inputBuffer)

			if (inputBufferValid <= 0) {
				throw EOFException()
			}
		} catch (ex: IOException) {
			inputBufferValid = 2
			inputBuffer[0] = 0xFF.toByte()
			inputBuffer[1] = 0xD9.toByte()    // EOI

			if (!isIgnoreIOerror) {
				throw ex
			}
		}

	}

	@Throws(IOException::class)
	private fun read(buf: ByteArray, off: Int, len: Int) {
		var off = off
		var len = len
		while (len > 0) {
			val avail = inputBufferValid - inputBufferPos
			if (avail == 0) {
				fetch()
				continue
			}
			val copy = if (avail > len) len else avail
			System.arraycopy(inputBuffer, inputBufferPos, buf, off, copy)
			off += copy
			len -= copy
			inputBufferPos += copy
		}
	}

	private val u8: Int
		@Throws(IOException::class)
		get() {
			if (inputBufferPos == inputBufferValid) {
				fetch()
			}
			return inputBuffer[inputBufferPos++].toInt() and 255
		}

	private val u16: Int
		@Throws(IOException::class)
		get() {
			val t = u8
			return t shl 8 or u8
		}

	@Throws(IOException::class)
	private fun skip(amount: Int) {
		var amount = amount
		while (amount > 0) {
			val inputBufferRemaining = inputBufferValid - inputBufferPos
			if (amount > inputBufferRemaining) {
				amount -= inputBufferRemaining
				fetch()
			} else {
				inputBufferPos += amount
				return
			}
		}
	}

	@Throws(IOException::class)
	private fun growBufferCheckMarker() {
		val c = u8
		if (c != 0) {
			marker = c
			nomore = true
		}
	}

	@Throws(IOException::class)
	private fun growBufferUnsafe() {
		do {
			var b = 0
			if (!nomore) {
				b = u8
				if (b == 0xff) {
					growBufferCheckMarker()
				}
			}
			codeBuffer = codeBuffer or (b shl 24 - codeBits)
			codeBits += 8
		} while (codeBits <= 24)
	}

	@Throws(IOException::class)
	private fun decode(h: Huffman): Int {
		if (codeBits < 16) {
			growBufferUnsafe()
		}
		val k = h.fast[codeBuffer.ushr(32 - Huffman.FAST_BITS)].toInt() and 255
		if (k < 0xFF) {
			val s = h.size[k].toInt()
			codeBuffer = codeBuffer shl s
			codeBits -= s
			return h.values[k].toInt() and 255
		}
		return decodeSlow(h)
	}

	@Throws(IOException::class)
	private fun decodeSlow(h: Huffman): Int {
		val temp = codeBuffer.ushr(16)
		var s = Huffman.FAST_BITS + 1

		while (temp >= h.maxCode[s]) {
			s++
		}

		val k = temp.ushr(16 - s) + h.delta[s]
		codeBuffer = codeBuffer shl s
		codeBits -= s
		return h.values[k].toInt() and 255
	}

	@Throws(IOException::class)
	private fun extendReceive(n: Int): Int {
		if (codeBits < 24) {
			growBufferUnsafe()
		}

		var k = codeBuffer.ushr(32 - n)
		codeBuffer = codeBuffer shl n
		codeBits -= n

		val limit = 1 shl n - 1
		if (k < limit) {
			k -= limit * 2 - 1
		}
		return k
	}

	@Throws(IOException::class)
	private fun decodeBlock(data: ShortArray, c: Component) {
		Arrays.fill(data, 0.toShort())

		val dq = c.dequant

		run {
			val t = decode(c.huffDC!!)
			var dc = c.dcPred
			if (t > 0) {
				dc += extendReceive(t)
				c.dcPred = dc
			}

			data[0] = (dc * (dq!![0].toInt() and 0xFF)).toShort()
		}

		val hac = c.huffAC

		var k = 1
		do {
			val rs = decode(hac!!)
			k += rs shr 4
			val s = rs and 15
			if (s != 0) {
				val v = extendReceive(s) * (dq!![k].toInt() and 0xFF)
				data[dezigzag[k].toInt()] = v.toShort()
			} else if (rs != 0xF0) {
				break
			}
		} while (++k < 64)
	}

	@Throws(IOException::class)
	private fun getMarker(): Int {
		var m = marker
		if (m != MARKER_NONE) {
			marker = MARKER_NONE
			return m
		}
		m = u8
		if (m != 0xFF) {
			return MARKER_NONE
		}
		do {
			m = u8
		} while (m == 0xFF)
		return m
	}

	private fun reset() {
		codeBits = 0
		codeBuffer = 0
		nomore = false
		marker = MARKER_NONE

		if (restartInterval != 0) {
			todo = restartInterval
		} else {
			todo = Integer.MAX_VALUE
		}

		for (c in components!!) {
			c!!.dcPred = 0
		}
	}

	@Throws(IOException::class)
	private fun checkRestart(): Boolean {
		if (codeBits < 24) {
			growBufferUnsafe()
		}
		if (marker >= 0xD0 && marker <= 0xD7) {
			reset()
			return true
		}
		return false
	}

	@Throws(IOException::class)
	private fun processMarker(marker: Int) {
		if (marker >= 0xE0 && (marker <= 0xEF || marker == 0xFE)) {
			val l = u16 - 2
			if (l < 0) {
				throw IOException("bad length")
			}
			skip(l)
			return
		}

		when (marker) {
			MARKER_NONE -> throw IOException("Expected marker")

			0xC2      // SOF - progressive
			-> throw IOException("Progressive JPEG not supported")

			0xDD      // DRI - specify restart interval
			-> {
				if (u16 != 4) {
					throw IOException("bad DRI length")
				}
				restartInterval = u16
			}

			0xDB -> {    // DQT - define dequant table
				var l = u16 - 2
				while (l >= 65) {
					val q = u8
					val p = q shr 4
					val t = q and 15
					if (p != 0) {
						throw IOException("bad DQT type")
					}
					if (t > 3) {
						throw IOException("bad DQT table")
					}
					read(dequant[t], 0, 64)
					l -= 65
				}
				if (l != 0) {
					throw IOException("bad DQT length")
				}
			}

			0xC4 -> {    // DHT - define huffman table
				var l = u16 - 2
				while (l > 17) {
					val q = u8
					val tc = q shr 4
					val th = q and 15
					if (tc > 1 || th > 3) {
						throw IOException("bad DHT header")
					}
					val tmp = idct2D.tmp2D   // reuse memory
					for (i in 0..15) {
						tmp[i] = u8
					}
					val h = Huffman(tmp)
					val m = h.numSymbols
					l -= 17 + m
					if (l < 0) {
						throw IOException("bad DHT length")
					}
					read(h.values, 0, m)
					huffmanTables[tc * 4 + th] = h
				}
				if (l != 0) {
					throw IOException("bad DHT length")
				}
			}

			else -> throw IOException("Unknown marker: " + Integer.toHexString(marker))
		}
	}

	@Throws(IOException::class)
	private fun skipPadding() {
		var x: Int
		do {
			x = u8
		} while (x == 0)

		if (x == 0xFF) {
			marker = u8
		}
	}

	@Throws(IOException::class)
	private fun processScanHeader() {
		val ls = u16
		val scanN = u8

		if (scanN < 1 || scanN > 4) {
			throw IOException("bad SOS component count")
		}
		if (ls != 6 + 2 * scanN) {
			throw IOException("bad SOS length")
		}

		order = arrayOfNulls<Component>(scanN)
		for (i in 0..scanN - 1) {
			val id = u8
			val q = u8
			for (c in components!!) {
				if (c!!.id == id) {
					val hd = q shr 4
					val ha = q and 15
					if (hd > 3 || ha > 3) {
						throw IOException("bad huffman table index")
					}
					c.huffDC = huffmanTables[hd]
					c.huffAC = huffmanTables[ha + 4]
					if (c.huffDC == null || c.huffAC == null) {
						throw IOException("bad huffman table index")
					}
					order!![i] = c
					break
				}
			}
			if (order!![i] == null) {
				throw IOException("unknown color component")
			}
		}

		if (u8 != 0) {
			throw IOException("bad SOS")
		}
		u8
		if (u8 != 0) {
			throw IOException("bad SOS")
		}
	}

	@Throws(IOException::class)
	private fun processSOF() {
		val lf = u16
		if (lf < 11) {
			throw IOException("bad SOF length")
		}

		if (u8 != 8) {
			throw IOException("only 8 bit JPEG supported")
		}

		imageHeight = u16
		imageWidth = u16

		if (imageWidth <= 0 || imageHeight <= 0) {
			throw IOException("Invalid image size")
		}

		val numComps = u8
		if (numComps != 3 && numComps != 1) {
			throw IOException("bad component count")
		}

		if (lf != 8 + 3 * numComps) {
			throw IOException("bad SOF length")
		}

		var hMax = 1
		var vMax = 1

		components = arrayOfNulls<Component>(numComps)
		for (i in 0..numComps - 1) {
			val c = Component(u8)
			val q = u8
			val tq = u8

			c.blocksPerMCUHorz = q shr 4
			c.blocksPerMCUVert = q and 15

			if (c.blocksPerMCUHorz == 0 || c.blocksPerMCUHorz > 4) throw IOException("bad H")
			if (c.blocksPerMCUVert == 0 || c.blocksPerMCUVert > 4) throw IOException("bad V")
			if (tq > 3) throw IOException("bad TQ")
			c.dequant = dequant[tq]

			hMax = Math.max(hMax, c.blocksPerMCUHorz)
			vMax = Math.max(vMax, c.blocksPerMCUVert)

			components!![i] = c
		}

		val mcuW = hMax * 8
		val mcuH = vMax * 8

		imgHMax = hMax
		imgVMax = vMax
		numMCUColumns = (imageWidth + mcuW - 1) / mcuW
		numMCURows = (imageHeight + mcuH - 1) / mcuH

		for (i in 0..numComps - 1) {
			val c = components!![i]!!
			c.width = (imageWidth * c.blocksPerMCUHorz + hMax - 1) / hMax
			c.height = (imageHeight * c.blocksPerMCUVert + vMax - 1) / vMax
			c.minReqWidth = numMCUColumns * c.blocksPerMCUHorz * 8
			c.minReqHeight = numMCURows * c.blocksPerMCUVert * 8

			if (c.blocksPerMCUHorz < hMax) c.upsampler = c.upsampler or 1
			if (c.blocksPerMCUVert < vMax) c.upsampler = c.upsampler or 2
		}
	}

	@Throws(IllegalStateException::class)
	private fun ensureHeaderDecoded() {
		if (!headerDecoded) {
			throw IllegalStateException("need to decode header first")
		}
	}

	private fun allocateDecodeTmp(YUVstride: Int): Boolean {
		if (decodeTmp == null) {
			decodeTmp = arrayOfNulls<ByteArray>(3)
		}

		var requiresUpsampling = false
		for (compIdx in 0..2) {
			val c = order!![compIdx]!!
			val reqSize = c.minReqWidth * c.blocksPerMCUVert * 8
			if (decodeTmp!![compIdx] == null || decodeTmp!![compIdx]!!.size < reqSize) {
				decodeTmp!![compIdx] = ByteArray(reqSize)
			}
			if (c.upsampler != 0) {
				if (upsampleTmp == null) upsampleTmp = arrayOfNulls<ByteArray>(3)
				val upsampleReq = imgVMax * 8 * YUVstride
				if (upsampleTmp!![compIdx] == null || upsampleTmp!![compIdx]!!.size < upsampleReq) {
					upsampleTmp!![compIdx] = ByteArray(upsampleReq)
				}
				requiresUpsampling = true
			}
		}
		return requiresUpsampling
	}

	@Throws(IOException::class)
	private fun decodeMCUrow() {
		++currentMCURow
		for (i in 0..numMCUColumns - 1) {
			for (compIdx in 0..2) {
				val c = order!![compIdx]!!
				val outStride = c.minReqWidth
				var outPosY = 8 * i * c.blocksPerMCUHorz

				var y = 0
				while (y < c.blocksPerMCUVert) {
					var x = 0
					var outPos = outPosY
					while (x < c.blocksPerMCUHorz) {
						try {
							decodeBlock(data, c)
						} catch (ex: ArrayIndexOutOfBoundsException) {
							throwBadHuffmanCode()
						}

						idct2D.compute(decodeTmp!![compIdx]!!, outPos, outStride, data)
						x++
						outPos += 8
					}
					y++
					outPosY += 8 * outStride
				}
			}
			if (--todo <= 0) {
				if (!checkRestart()) {
					break
				}
			}
		}
	}

	private fun doUpsampling(YUVstride: Int) {
		for (compIdx in 0..2) {
			val c = order!![compIdx]!!
			val inStride = c.minReqWidth
			val height = c.blocksPerMCUVert * 8
			when (c.upsampler) {
				1 -> for (i in 0..height - 1) {
					upsampleH2(upsampleTmp!![compIdx]!!, i * YUVstride, decodeTmp!![compIdx]!!, i * inStride, c.width)
				}

				2 -> {
					run {
						var i = 0
						var inPos0 = 0
						var inPos1 = 0
						while (i < height) {
							upsampleV2(upsampleTmp!![compIdx]!!, i * 2 * YUVstride, decodeTmp!![compIdx]!!, inPos0, inPos1, c.width)
							upsampleV2(upsampleTmp!![compIdx]!!, (i * 2 + 1) * YUVstride, decodeTmp!![compIdx]!!, inPos1, inPos0, c.width)
							inPos0 = inPos1
							inPos1 += inStride
							i++
						}
					}
					var i = 0
					var inPos0 = 0
					var inPos1 = 0
					while (i < height) {
						upsampleHV2(upsampleTmp!![compIdx]!!, i * 2 * YUVstride, decodeTmp!![compIdx]!!, inPos0, inPos1, c.width)
						upsampleHV2(upsampleTmp!![compIdx]!!, (i * 2 + 1) * YUVstride, decodeTmp!![compIdx]!!, inPos1, inPos0, c.width)
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
						upsampleHV2(upsampleTmp!![compIdx]!!, i * 2 * YUVstride, decodeTmp!![compIdx]!!, inPos0, inPos1, c.width)
						upsampleHV2(upsampleTmp!![compIdx]!!, (i * 2 + 1) * YUVstride, decodeTmp!![compIdx]!!, inPos1, inPos0, c.width)
						inPos0 = inPos1
						inPos1 += inStride
						i++
					}
				}
			}
		}
	}

	class Huffman @Throws(IOException::class)
	constructor(count: IntArray) {

		internal val fast: ByteArray
		internal val values: ByteArray
		internal val size: ByteArray
		internal val maxCode: IntArray
		internal val delta: IntArray

		init {
			var numSymbols = 0
			for (i in 0..15) numSymbols += count[i]

			fast = ByteArray(1 shl FAST_BITS)
			values = ByteArray(numSymbols)
			size = ByteArray(numSymbols)
			maxCode = IntArray(18)
			delta = IntArray(17)

			run {
				var i = 0
				var k = 0
				while (i < 16) {
					for (j in 0..count[i] - 1) {
						size[k++] = (i + 1).toByte()
					}
					i++
				}
			}

			val code = IntArray(256)

			var i = 1
			var k = 0
			run {
				var c = 0
				while (i <= 16) {
					delta[i] = k - c
					if (k < numSymbols && size[k].toInt() == i) {
						do {
							code[k++] = c++
						} while (k < numSymbols && size[k].toInt() == i)
						if (c - 1 >= 1 shl i) {
							throw IOException("Bad code length")
						}
					}
					maxCode[i] = c shl 16 - i
					c = c shl 1
					i++
				}
			}
			maxCode[i] = Integer.MAX_VALUE

			Arrays.fill(fast, (-1).toByte())
			i = 0
			while (i < k) {
				val s = size[i].toInt()
				if (s <= FAST_BITS) {
					val c = code[i] shl FAST_BITS - s
					val m = 1 shl FAST_BITS - s
					for (j in 0..m - 1) {
						fast[c + j] = i.toByte()
					}
				}
				i++
			}
		}

		val numSymbols: Int
			get() = values.size

		companion object {

			internal val FAST_BITS = 9
			internal val FAST_MASK = (1 shl FAST_BITS) - 1
		}
	}

	class IDCT_2D {

		internal val tmp2D = IntArray(64)

		private fun computeV(data: ShortArray) {
			val tmp = tmp2D

			var i = 0
			do {
				val s0 = data[i + 0].toInt()
				val s1 = data[i + 8].toInt()
				val s2 = data[i + 16].toInt()
				val s3 = data[i + 24].toInt()
				val s4 = data[i + 32].toInt()
				val s5 = data[i + 40].toInt()
				val s6 = data[i + 48].toInt()
				val s7 = data[i + 56].toInt()

				var p1: Int
				var p2: Int
				var p3: Int
				var p4: Int
				var p5: Int

				p1 = (s2 + s6) * C0
				p2 = (s0 + s4 shl 12) + 512
				p3 = (s0 - s4 shl 12) + 512
				p4 = p1 + s6 * C1
				p5 = p1 + s2 * C2

				val x0 = p2 + p5
				val x3 = p2 - p5
				val x1 = p3 + p4
				val x2 = p3 - p4

				p1 = s7 + s1
				p2 = s5 + s3
				p3 = s7 + s3
				p4 = s5 + s1
				p5 = (p3 + p4) * C3

				p1 = p5 + p1 * C8
				p2 = p5 + p2 * C9
				p3 = p3 * C10
				p4 = p4 * C11

				val t0 = s7 * C4 + p1 + p3
				val t1 = s5 * C5 + p2 + p4
				val t2 = s3 * C6 + p2 + p3
				val t3 = s1 * C7 + p1 + p4

				tmp[i] = x0 + t3 shr 10
				tmp[i + 56] = x0 - t3 shr 10
				tmp[i + 8] = x1 + t2 shr 10
				tmp[i + 48] = x1 - t2 shr 10
				tmp[i + 16] = x2 + t1 shr 10
				tmp[i + 40] = x2 - t1 shr 10
				tmp[i + 24] = x3 + t0 shr 10
				tmp[i + 32] = x3 - t0 shr 10
			} while (++i < 8)
		}

		fun compute(out: ByteBuffer, outPos: Int, outStride: Int, data: ShortArray) {
			var outPos = outPos
			computeV(data)

			val tmp = tmp2D
			var i = 0
			while (i < 64) {
				val s0 = tmp[i] + (257 shl 4)
				val s1 = tmp[i + 1]
				val s2 = tmp[i + 2]
				val s3 = tmp[i + 3]
				val s4 = tmp[i + 4]
				val s5 = tmp[i + 5]
				val s6 = tmp[i + 6]
				val s7 = tmp[i + 7]

				var p1: Int
				var p2: Int
				var p3: Int
				var p4: Int
				var p5: Int

				p1 = (s2 + s6) * C0
				p2 = s0 + s4 shl 12
				p3 = s0 - s4 shl 12
				p4 = p1 + s6 * C1
				p5 = p1 + s2 * C2

				val x0 = p2 + p5
				val x3 = p2 - p5
				val x1 = p3 + p4
				val x2 = p3 - p4

				p1 = s7 + s1
				p2 = s5 + s3
				p3 = s7 + s3
				p4 = s5 + s1
				p5 = (p3 + p4) * C3

				p1 = p5 + p1 * C8
				p2 = p5 + p2 * C9
				p3 = p3 * C10
				p4 = p4 * C11

				val t0 = s7 * C4 + p1 + p3
				val t1 = s5 * C5 + p2 + p4
				val t2 = s3 * C6 + p2 + p3
				val t3 = s1 * C7 + p1 + p4

				out.put(outPos + 0, clampShift17(x0 + t3))
				out.put(outPos + 7, clampShift17(x0 - t3))
				out.put(outPos + 1, clampShift17(x1 + t2))
				out.put(outPos + 6, clampShift17(x1 - t2))
				out.put(outPos + 2, clampShift17(x2 + t1))
				out.put(outPos + 5, clampShift17(x2 - t1))
				out.put(outPos + 3, clampShift17(x3 + t0))
				out.put(outPos + 4, clampShift17(x3 - t0))

				outPos += outStride
				i += 8
			}
		}

		fun compute(out: ByteArray, outPos: Int, outStride: Int, data: ShortArray) {
			var outPos = outPos
			computeV(data)

			val tmp = tmp2D
			var i = 0
			while (i < 64) {
				val s0 = tmp[i + 0] + (257 shl 4)
				val s1 = tmp[i + 1]
				val s2 = tmp[i + 2]
				val s3 = tmp[i + 3]
				val s4 = tmp[i + 4]
				val s5 = tmp[i + 5]
				val s6 = tmp[i + 6]
				val s7 = tmp[i + 7]

				var p1: Int
				var p2: Int
				var p3: Int
				var p4: Int
				var p5: Int

				p1 = (s2 + s6) * C0
				p2 = s0 + s4 shl 12
				p3 = s0 - s4 shl 12
				p4 = p1 + s6 * C1
				p5 = p1 + s2 * C2

				val x0 = p2 + p5
				val x3 = p2 - p5
				val x1 = p3 + p4
				val x2 = p3 - p4

				p1 = s7 + s1
				p2 = s5 + s3
				p3 = s7 + s3
				p4 = s5 + s1
				p5 = (p3 + p4) * C3

				p1 = p5 + p1 * C8
				p2 = p5 + p2 * C9
				p3 = p3 * C10
				p4 = p4 * C11

				val t0 = s7 * C4 + p1 + p3
				val t1 = s5 * C5 + p2 + p4
				val t2 = s3 * C6 + p2 + p3
				val t3 = s1 * C7 + p1 + p4

				out[outPos] = clampShift17(x0 + t3)
				out[outPos + 7] = clampShift17(x0 - t3)
				out[outPos + 1] = clampShift17(x1 + t2)
				out[outPos + 6] = clampShift17(x1 - t2)
				out[outPos + 2] = clampShift17(x2 + t1)
				out[outPos + 5] = clampShift17(x2 - t1)
				out[outPos + 3] = clampShift17(x3 + t0)
				out[outPos + 4] = clampShift17(x3 - t0)

				outPos += outStride
				i += 8
			}
		}

		companion object {

			private val C0 = f2f(0.541196100)
			private val C1 = f2f(-1.847759065)
			private val C2 = f2f(0.765366865)
			private val C3 = f2f(1.175875602)
			private val C4 = f2f(0.298631336)
			private val C5 = f2f(2.053119869)
			private val C6 = f2f(3.072711026)
			private val C7 = f2f(1.501321110)
			private val C8 = f2f(-0.899976223)
			private val C9 = f2f(-2.562915447)
			private val C10 = f2f(-1.961570560)
			private val C11 = f2f(-0.390180644)

			private fun clampShift17(x: Int): Byte {
				if (x < 0) return 0
				if (x > 255 shl 17) return 255.toByte()
				return x.ushr(17).toByte()
			}

			@Strictfp private fun f2f(x: Double): Int {
				return Math.round(Math.scalb(x, 12)).toInt()
			}
		}
	}

	class Component internal constructor(val id: Int) {

		internal var dcPred: Int = 0
		internal var huffDC: Huffman? = null
		internal var huffAC: Huffman? = null
		internal var dequant: ByteArray? = null
		var blocksPerMCUVert: Int = 0
			internal set
		var blocksPerMCUHorz: Int = 0
			internal set
		var width: Int = 0
			internal set
		var height: Int = 0
			internal set
		var minReqWidth: Int = 0
			internal set
		var minReqHeight: Int = 0
			internal set
		internal var outPos: Int = 0
		internal var upsampler: Int = 0
	}

	companion object {
		internal val MARKER_NONE = 0xFF

		@Throws(IOException::class)
		private fun throwBadHuffmanCode() {
			throw IOException("Bad huffman code")
		}

		private fun YUVtoRGB(out: ByteBuffer, outPos: Int, inY: ByteArray, inU: ByteArray, inV: ByteArray, inPos: Int, count: Int) {
			var outPos = outPos
			var inPos = inPos
			var count = count
			do {
				val y = inY[inPos].toInt() and 255
				val u = (inU[inPos].toInt() and 255) - 128
				val v = (inV[inPos].toInt() and 255) - 128
				var r = y + (32768 + v * 91881 shr 16)
				var g = y + (32768 - v * 46802 - u * 22554 shr 16)
				var b = y + (32768 + u * 116130 shr 16)
				if (r > 255)
					r = 255
				else if (r < 0) r = 0
				if (g > 255)
					g = 255
				else if (g < 0) g = 0
				if (b > 255)
					b = 255
				else if (b < 0) b = 0
				out.put(outPos + 0, r.toByte())
				out.put(outPos + 1, g.toByte())
				out.put(outPos + 2, b.toByte())
				out.put(outPos + 3, 255.toByte())
				outPos += 4
				inPos++
			} while (--count > 0)
		}

		private fun upsampleH2(out: ByteArray, outPos: Int, `in`: ByteArray, inPos: Int, width: Int) {
			if (width == 1) {
				out[outPos + 1] = `in`[inPos]
				out[outPos] = out[outPos + 1]
			} else {
				var i0 = `in`[inPos].toInt() and 255
				var i1 = `in`[inPos + 1].toInt() and 255
				out[outPos] = i0.toByte()
				out[outPos + 1] = (i0 * 3 + i1 + 2 shr 2).toByte()
				for (i in 2..width - 1) {
					val i2 = `in`[inPos + i].toInt() and 255
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

		private fun upsampleV2(out: ByteArray, outPos: Int, `in`: ByteArray, inPos0: Int, inPos1: Int, width: Int) {
			for (i in 0..width - 1) {
				out[outPos + i] = (3 * (`in`[inPos0 + i].toInt() and 255) + (`in`[inPos1 + i].toInt() and 255) + 2 shr 2).toByte()
			}
		}

		private fun upsampleHV2(out: ByteArray, outPos: Int, `in`: ByteArray, inPos0: Int, inPos1: Int, width: Int) {
			if (width == 1) {
				val i0 = `in`[inPos0].toInt() and 255
				val i1 = `in`[inPos1].toInt() and 255
				out[outPos + 1] = (i0 * 3 + i1 + 2 shr 2).toByte()
				out[outPos] = out[outPos + 1]
			} else {
				var i1 = 3 * (`in`[inPos0].toInt() and 255) + (`in`[inPos1].toInt() and 255)
				out[outPos] = (i1 + 2 shr 2).toByte()
				for (i in 1..width - 1) {
					val i0 = i1
					i1 = 3 * (`in`[inPos0 + i].toInt() and 255) + (`in`[inPos1 + i].toInt() and 255)
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
			"\u003f\u003f\u003f\u003f\u003f\u003f\u003f").toCharArray()
	}
}