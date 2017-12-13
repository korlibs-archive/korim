package com.soywiz.korim.font.ttf

import com.soywiz.klock.DateTime
import com.soywiz.klock.years
import com.soywiz.kmem.readS16_LEBE
import com.soywiz.kmem.write16_LEBE
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
import com.soywiz.korim.vector.filled
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.hex
import com.soywiz.korma.ds.IntArrayList

// Used information from:
// - https://www.sweetscape.com/010editor/repository/files/TTF.bt
// - http://scripts.sil.org/cms/scripts/page.php?site_id=nrsi&id=iws-chapter08
// - https://www.microsoft.com/en-us/Typography/OpenTypeSpecification.aspx
// - https://en.wikipedia.org/wiki/Em_(typography)
// - http://stevehanov.ca/blog/index.php?id=143 (Let's read a Truetype font file from scratch)
// - http://chanae.walon.org/pub/ttf/ttf_glyphs.htm
class TtfFontReader private constructor(val s: SyncStream) {
	data class Table(val id: String, val checksum: Int, val offset: Int, val length: Int) {
		lateinit var s: SyncStream

		fun open() = s.clone()
	}

	enum class NameIds(val id: Int) {
		COPYRIGHT(0), FONT_FAMILY_NAME(1), FONT_SUBFAMILY_NAME(2), UNIQUE_FONT_ID(3),
		FULL_FONT_NAME(4), VERSION_STRING(5), POSTSCRIPT_NAME(6), TRADEMARK(7),
		MANUFACTURER(8), DESIGNER(9), DESCRIPTION(10), URL_VENDOR(11),
		URL_DESIGNER(12), LICENSE_DESCRIPTION(13), LICENSE_URL(14), RESERVED_15(15),
		TYPO_FAMILY_NAME(16), TYPO_SUBFAMILY_NAME(17), COMPATIBLE_FULL(18), SAMPLE_TEXT(19),
		POSTSCRIPT_CID(20), WWS_FAMILY_NAME(21), WWS_SUBFAMILY_NAME(22), LIGHT_BACKGROUND_PALETTE(23),
		DARK_BACKGROUND_PALETTE(24), VARIATIONS_POSTSCRIPT_NAME_PREFIX(25);

		companion object {
			val names = values()
		}
	}

	val tablesByName = LinkedHashMap<String, Table>()

	var numGlyphs = 0
	var maxPoints = 0
	var maxContours = 0
	var maxCompositePoints = 0
	var maxCompositeContours = 0
	var maxZones = 0
	var maxTwilightPoints = 0
	var maxStorage = 0
	var maxFunctionDefs = 0
	var maxInstructionDefs = 0
	var maxStackElements = 0
	var maxSizeOfInstructions = 0
	var maxComponentElements = 0
	var maxComponentDepth = 0
	var locs = IntArray(0)

	var fontRev = Fixed(0, 0)
	var unitsPerEm = 128
	var xMin = 0
	var yMin = 0
	var xMax = 0
	var yMax = 0

	var indexToLocFormat = 0
	var glyphDataFormat = 0

	companion object {
		operator fun invoke(s: SyncStream): TtfFontReader {
			return TtfFontReader(s)
		}
	}

	init {
		s.slice().readHeader()
		readHead()
		readMaxp()
		readNames()
		readLoca()
		//readGlyphs()
	}

	//val tablesByName =

	fun SyncStream.readHeader() {
		val majorVersion = readU16_be().apply { if (this != 1) invalidOp("Not a TTF file") }
		val minorVersion = readU16_be().apply { if (this != 0) invalidOp("Not a TTF file") }
		val numTables = readU16_be()
		val searchRange = readU16_be()
		val entrySelector = readU16_be()
		val rangeShift = readU16_be()

		val tables = (0 until numTables).map {
			Table(readStringz(4), readS32_be(), readS32_be(), readS32_be())
		}

		for (table in tables) {
			table.s = sliceWithSize(table.offset, table.length)
			tablesByName[table.id] = table
		}

		//for (table in tables) println(table)
	}

	fun readNames() = tablesByName["name"]?.open()?.run {
		val format = readU16_be()
		val count = readU16_be()
		val stringOffset = readU16_be()
		for (n in 0 until count) {
			val platformId = readU16_be()
			val encodingId = readU16_be()
			val languageId = readU16_be()
			val nameId = readU16_be()
			val length = readU16_be()
			val offset = readU16_be()

			val charset = when (encodingId) {
				0 -> UTF8
				1 -> UTF16_BE
				else -> UTF16_BE
			}
			//println("" + (stringOffset.toLong() + offset) + " : " + length + " : " + charset)
			val string = this.clone().sliceWithSize(stringOffset.toLong() + offset, length.toLong()).readAll().toString(charset)
			//println(string)
		}
	}

	fun getGlyphOffset(index: Int) = locs[index + 1]
	fun getGlyphSize(index: Int) = locs[index + 2] - locs[index + 1]

	fun readLoca() {
		val s = tablesByName["loca"]?.open() ?: return
		val bytesPerEntry = when (indexToLocFormat) {
			0 -> 2
			1 -> 4
			else -> invalidOp
		}

		val data = s.readBytesExact(bytesPerEntry * (numGlyphs + 1))
		locs = IntArray(numGlyphs + 1)

		FastByteArrayInputStream(data).run {
			when (indexToLocFormat) {
				0 -> run { for (n in locs.indices) locs[n] = readU16_be() * 2 }
				1 -> run { for (n in locs.indices) locs[n] = readS32_be() * 2 }
				else -> invalidOp
			}
		}
		println("locs: ${locs.toList()}")
	}

	data class Fixed(val num: Int, val den: Int)

	fun SyncStream.readFixed() = Fixed(readS16_le(), readS16_le())

	fun readHead() = tablesByName["head"]?.open()?.run {
		readU16_be().apply { if (this != 1) invalidOp("Invalid TTF") }
		readU16_be().apply { if (this != 0) invalidOp("Invalid TTF") }
		fontRev = readFixed()
		val checkSumAdjustment = readS32_be()
		readS32_be().apply { if (this != 0x5F0F3CF5) invalidOp("Invalid magic ${this.hex}") }
		val flags = readU16_be()
		unitsPerEm = readU16_be()
		val created = readS64_be() * 1000L
		val modified = readS64_be() * 1000L
		xMin = readS16_be()
		yMin = readS16_be()
		xMax = readS16_be()
		yMax = readS16_be()
		val macStyle = readU16_be()
		val lowestRecPPEM = readU16_be()
		val fontDirectionHint = readS16_be()
		indexToLocFormat = readS16_be() // 0=Int16, 1=Int32
		glyphDataFormat = readS16_be()

		println("unitsPerEm: $unitsPerEm")
		println("created: ${DateTime(created) - 76.years}")
		println("modified: ${DateTime(modified) - 76.years}")
		println("bounds: ($xMin, $yMin)-($xMax, $yMax)")
	}

	fun readMaxp() = tablesByName["maxp"]?.open()?.run {
		val version = readFixed()
		numGlyphs = readU16_be()
		maxPoints = readU16_be()
		maxContours = readU16_be()
		maxCompositePoints = readU16_be()
		maxCompositeContours = readU16_be()
		maxZones = readU16_be()
		maxTwilightPoints = readU16_be()
		maxStorage = readU16_be()
		maxFunctionDefs = readU16_be()
		maxInstructionDefs = readU16_be()
		maxStackElements = readU16_be()
		maxSizeOfInstructions = readU16_be()
		maxComponentElements = readU16_be()
		maxComponentDepth = readU16_be()
	}

	fun readGlyphs() {
		val s = tablesByName["glyf"]?.open() ?: return
		println("numGlyphs: $numGlyphs")
		for (n in 0 until numGlyphs - 1) {
			val offset = getGlyphOffset(n)
			val size = getGlyphSize(n)
			//println("offset: $offset, size: $size")
			val glyph = s.sliceWithSize(offset, size).readGlyph()
			//val gp = glyph.createGraphicsPath()
			//showImageAndWait(gp.filled(Context2d.Color(Colors.RED)))
			//println(glyph)
		}
	}

	suspend fun readGlyphsSuspend() {
		val s = tablesByName["glyf"]?.open() ?: return
		println(this@TtfFontReader)
		println("numGlyphs: $numGlyphs")
		println("locs: ${locs.toList()}")
		for (n in 0 until numGlyphs - 1) {
			val offset = getGlyphOffset(n)
			val size = getGlyphSize(n)
			//println("offset: $offset, size: $size")
			val glyph = s.sliceWithSize(offset, size).readGlyph()
			val gp = glyph.createGraphicsPath()

			var scale = 64.0 / unitsPerEm.toDouble()
			var width = xMax - xMin
			var height = yMax - yMin

			println(glyph)
			showImageAndWait(
				//gp.scaled(scale, -scale).translated(-xMin, -yMin - height).filled(Context2d.Color(Colors.RED))
				gp.filled(Context2d.Color(Colors.RED))
			)
		}
	}

	fun Glyph.createGraphicsPath(): GraphicsPath {
		val p = GraphicsPath()

		for (n in 0 until contoursIndices.size - 1) {
			val cstart = contoursIndices[n] + 1
			val cend = contoursIndices[n + 1]
			val csize = cend - cstart + 1

			var curr: Contour = contour(cend)
			var next: Contour = contour(cstart)

			if (curr.onCurve) {
				p.moveTo(curr.x, curr.y)
			} else {
				if (next.onCurve) {
					p.moveTo(next.x, next.y)
				} else {
					p.moveTo((curr.x + next.x) * 0.5.toInt(), ((curr.y + next.y) * 0.5).toInt())
				}
			}

			for (cpos in 0 until csize) {
				val prev = curr
				curr = next
				next = contour(cstart + ((cpos + 1) % csize))

				if (curr.onCurve) {
					p.lineTo(curr.x, curr.y)
				} else {
					var prev2X = prev.x
					var prev2Y = prev.y
					var next2X = next.x
					var next2Y = next.y

					if (!prev.onCurve) {
						prev2X = ((curr.x + prev.x) * 0.5).toInt()
						prev2Y = ((curr.y + prev.y) * 0.5).toInt()
						p.lineTo(prev2X, prev2Y)
					}

					if (!next.onCurve) {
						next2X = ((curr.x + next.x) * 0.5).toInt()
						next2Y = ((curr.y + next.y) * 0.5).toInt()
					}

					p.lineTo(prev2X, prev2Y)
					p.quadTo(curr.x, curr.y, next2X, next2Y)
				}
			}

			p.close()
		}

		return p
	}

	interface IGlyph

	data class Contour(var x: Int = 0, var y: Int = 0, var onCurve: Boolean = false) {
		fun copyFrom(that: Contour) {
			this.x = that.x
			this.y = that.y
			this.onCurve = that.onCurve
		}
	}

	data class Glyph(
		val xMin: Int, val yMin: Int,
		val xMax: Int, val yMax: Int,
		val contoursIndices: IntArray,
		val flags: IntArray,
		val xPos: IntArray,
		val yPos: IntArray
	) : IGlyph {
		val npoints: Int get() = xPos.size
		fun onCurve(n: Int) = (flags[n] and 1) != 0
		fun contour(n: Int, out: Contour = Contour()) = out.apply {
			x = xPos[n]
			y = yPos[n]
			onCurve = onCurve(n)
		}
	}

	fun SyncStream.readGlyph(): Glyph {
		val ncontours = readS16_be()
		val xMin = readS16_be()
		val yMin = readS16_be()
		val xMax = readS16_be()
		val yMax = readS16_be()

		if (ncontours < 0) {
			TODO("readCompositeGlyph")
		} else {
			val contoursIndices = IntArray(ncontours + 1)
			contoursIndices[0] = -1
			for (n in 1..ncontours) contoursIndices[n] = readU16_be()
			val instructionLength = readU16_be()
			val instructions = readBytesExact(instructionLength)
			val numPoints = contoursIndices.lastOrNull()?.toInt()?.plus(1) ?: 0
			val flags = IntArrayList()

			var npos = 0
			while (npos < numPoints) {
				val cf = readU8()
				flags.add(cf)
				// Repeat
				if ((cf and 8) != 0) {
					val count = readU8()
					for (n in 0 until count) flags.add(cf)
					npos += count + 1
				} else {
					npos++
				}
			}

			val xPos = IntArray(numPoints)
			val yPos = IntArray(numPoints)

			//println("--------------: $numPoints flags=${flags.toList()}")

			for (xy in 0..1) {
				val pos = if (xy == 0) xPos else yPos
				val name = if (xy == 0) "x" else "y"
				var p = 0
				for (n in 0 until numPoints) {
					val flag = flags[n]
					val b1 = ((flag ushr (1 + xy)) and 1) != 0
					val b2 = ((flag ushr (4 + xy)) and 1) != 0
					if (b1) {
						// UBYTE: b2 represents sign
						val magnitude = readU8()
						if (b2) p += magnitude else p -= magnitude
						//println("UBYTE[$name]: $p")
					} else {
						// SHORT: b2==1 (use previous position), b2==0 SHORT
						if (!b2) {
							p += readS16_be()
							//println("SHORT[$name]: $p")
						} else {
							//println("PREV[$name]: $p")
						}
					}
					pos[n] = p
				}
			}

			//println("$ncontours, $xMin, $yMax, $xMax, $yMax, ${endPtsOfContours.toList()}, $numPoints, ${flags.toList()}")
			//println(xPos.toList())
			//println(yPos.toList())
			return Glyph(xMin, yMin, xMax, yMax, contoursIndices, flags.data.copyOf(flags.size), xPos, yPos)
		}
	}
}

// @TODO: Use the one from korio after korio >= 0.18.4
class UTF16Charset(val le: Boolean) : Charset("UTF-16-" + (if (le) "LE" else "BE")) {
	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
		for (n in start until end step 2) out.append(src.readS16_LEBE(n, le).toChar())
	}

	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
		val temp = ByteArray(2)
		for (n in start until end) {
			temp.write16_LEBE(0, src[n].toInt(), le)
			out.append(temp)
		}
	}
}

private val UTF16_LE = UTF16Charset(le = true)
private val UTF16_BE = UTF16Charset(le = false)
