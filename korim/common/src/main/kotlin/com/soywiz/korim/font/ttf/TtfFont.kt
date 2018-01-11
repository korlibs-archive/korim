package com.soywiz.korim.font.ttf

import com.soywiz.kds.IntArrayList
import com.soywiz.klock.DateTime
import com.soywiz.klock.years
import com.soywiz.kmem.toUnsigned
import com.soywiz.korim.color.Colors
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.UTF16_BE
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.hex

// Used information from:
// - https://www.sweetscape.com/010editor/repository/files/TTF.bt
// - http://scripts.sil.org/cms/scripts/page.php?site_id=nrsi&id=iws-chapter08
// - https://www.microsoft.com/en-us/Typography/OpenTypeSpecification.aspx
// - https://en.wikipedia.org/wiki/Em_(typography)
// - http://stevehanov.ca/blog/index.php?id=143 (Let's read a Truetype font file from scratch)
// - http://chanae.walon.org/pub/ttf/ttf_glyphs.htm
class TtfFont private constructor(val s: SyncStream) {
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

	data class Fixed(val num: Int, val den: Int)

	fun SyncStream.readFixed() = Fixed(readS16_le(), readS16_le())
	data class HorMetric(val advanceWidth: Int, val lsb: Int)

	val tablesByName = LinkedHashMap<String, Table>()
	fun openTable(name: String) = tablesByName[name]?.open()

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

	var hheaVersion = Fixed(0, 0)
	var ascender = 0
	var descender = 0
	var lineGap = 0
	var advanceWidthMax = 0
	var minLeftSideBearing = 0
	var minRightSideBearing = 0
	var xMaxExtent = 0
	var caretSlopeRise = 0
	var caretSlopeRun = 0
	var caretOffset = 0
	var metricDataFormat = 0
	var numberOfHMetrics = 0

	var locs = IntArray(0)

	var fontRev = Fixed(0, 0)
	var unitsPerEm = 128
	var xMin = 0
	var yMin = 0
	var xMax = 0
	var yMax = 0
	var macStyle = 0
	var lowestRecPPEM = 0
	var fontDirectionHint = 0

	var indexToLocFormat = 0
	var glyphDataFormat = 0

	var horMetrics = listOf<HorMetric>()
	val characterMaps = LinkedHashMap<Int, Int>()

	companion object {
		operator fun invoke(s: SyncStream): TtfFont {
			return TtfFont(s)
		}
	}

	init {
		readHeaderTables()
		readHead()
		readMaxp()
		readHhea()
		readNames()
		readLoca()
		readCmap()
		readHmtx()
	}

	fun readHeaderTables() = s.slice().apply {
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

	fun readNames() = openTable("name")?.run {
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

	fun readLoca() = openTable("loca")?.run {
		val bytesPerEntry = when (indexToLocFormat) {
			0 -> 2
			1 -> 4
			else -> invalidOp
		}

		val data = readBytesExact(bytesPerEntry * (numGlyphs + 1))
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

	fun readHead() = openTable("head")?.run {
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
		macStyle = readU16_be()
		lowestRecPPEM = readU16_be()
		fontDirectionHint = readS16_be()
		indexToLocFormat = readS16_be() // 0=Int16, 1=Int32
		glyphDataFormat = readS16_be()

		println("unitsPerEm: $unitsPerEm")
		println("created: ${DateTime(created) - 76.years}")
		println("modified: ${DateTime(modified) - 76.years}")
		println("bounds: ($xMin, $yMin)-($xMax, $yMax)")
	}

	fun readMaxp() = openTable("maxp")?.run {
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

	fun readHhea() = openTable("hhea")?.run {
		hheaVersion = readFixed()
		ascender = readS16_be()
		descender = readS16_be()
		lineGap = readS16_be()
		advanceWidthMax = readU16_be()
		minLeftSideBearing = readS16_be()
		minRightSideBearing = readS16_be()
		xMaxExtent = readS16_be()
		caretSlopeRise = readS16_be()
		caretSlopeRun = readS16_be()
		caretOffset = readS16_be()
		readS16_be() // reserved
		readS16_be() // reserved
		readS16_be() // reserved
		readS16_be() // reserved
		metricDataFormat = readS16_be()
		numberOfHMetrics = readU16_be()
	}

	fun readHmtx() = openTable("hmtx")?.run {
		val firstMetrics = (0 until numberOfHMetrics).map { HorMetric(readU16_be(), readS16_be()) }
		val lastAdvanceWidth = firstMetrics.last().advanceWidth
		val compressedMetrics = (0 until (numGlyphs - numberOfHMetrics)).map { HorMetric(lastAdvanceWidth, readS16_be()) }
		horMetrics = firstMetrics + compressedMetrics
	}

	fun readCmap() = openTable("cmap")?.run {
		data class EncodingRecord(val platformId: Int, val encodingId: Int, val offset: Int)

		val version = readU16_be()
		val numTables = readU16_be()
		val tables = (0 until numTables).map { EncodingRecord(readU16_be(), readU16_be(), readS32_be()) }

		for (table in tables) {
			sliceWithStart(table.offset.toLong()).run {
				val format = readU16_be()
				when (format) {
					4 -> {
						val length = readU16_be()
						//s.readStream(length - 4).run {
						val language = readU16_be()
						val segCount = readU16_be() / 2
						val searchRangeS = readU16_be()
						val entrySelector = readU16_be()
						val rangeShift = readU16_be()
						val endCount = readCharArray_be(segCount)
						readU16_be() // reserved
						val startCount = readCharArray_be(segCount)
						val idDelta = readShortArray_be(segCount)
						val rangeOffsetPos = position.toInt()
						val idRangeOffset = readCharArray_be(segCount)
						//val glyphIdArray = readCharArray_be(idRangeOffset.max()?.toInt() ?: 0)

						//println("$language")

						for (n in 0 until segCount) {
							val ec = endCount[n].toInt()
							val sc = startCount[n].toInt()
							val delta = idDelta[n].toInt()
							val iro = idRangeOffset[n].toInt()
							//println("%04X-%04X : %d : %d".format(sc, ec, delta, iro))
							for (c in sc..ec) {
								var index: Int = 0
								if (iro != 0) {
									var glyphIndexOffset = rangeOffsetPos + n * 2
									glyphIndexOffset += iro
									glyphIndexOffset += (c - sc) * 2
									index = sliceWithStart(glyphIndexOffset.toLong()).readU16_be()
									if (index != 0) {
										index += delta
									}
								} else {
									index = c + delta
								}
								characterMaps[c] = index and 0xFFFF
								//println("%04X --> %d".format(c, index and 0xFFFF))
							}
						}

						//for ((c, index) in characterMaps) println("\\u%04X -> %d".format(c.toInt(), index))
					}
					12 -> {
						readU16_be() // reserved
						val length = readS32_be()
						val language = readS32_be()
						val numGroups = readS32_be()

						for (n in 0 until numGroups) {
							val startCharCode = readS32_be()
							val endCharCode = readS32_be()
							val startGlyphId = readS32_be()

							var glyphId = startGlyphId
							for (c in startCharCode..endCharCode) {
								characterMaps[c] = glyphId++
							}
						}
					}
					else -> { // Ignored

					}
				}
				println("cmap.table.format: $format")
			}
		}
		println(tables)
	}

	fun getCharIndexFromCodePoint(codePoint: Int): Int? = characterMaps[codePoint]
	fun getCharIndexFromChar(char: Char): Int? = characterMaps[char.toInt()]

	fun getGlyphByCodePoint(codePoint: Int): IGlyph? = characterMaps[codePoint]?.let { getGlyphByIndex(it) }
	fun getGlyphByChar(char: Char): IGlyph? = getGlyphByCodePoint(char.toInt())

	fun getGlyphByIndex(index: Int): IGlyph? = openTable("glyf")?.run {
		val start = locs.getOrNull(index)?.toUnsigned() ?: 0
		val end = locs.getOrNull(index + 1)?.toUnsigned() ?: start
		val size = end - start
		if (size != 0L) {
			sliceWithStart(start).readGlyph(index)
		} else {
			Glyph(0, 0, 0, 0, intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf(), horMetrics[index].advanceWidth)
		}
	}

	fun getAllGlyphs() = (0 until numGlyphs).map { getGlyphByIndex(it) }.filterNotNull()

	interface IGlyph {
		val xMin: Int
		val yMin: Int
		val xMax: Int
		val yMax: Int
		val advanceWidth: Int
		fun fill(c: Context2d, size: Double, origin: Origin, color: Int)
	}

	data class Contour(var x: Int = 0, var y: Int = 0, var onCurve: Boolean = false) {
		fun copyFrom(that: Contour) {
			this.x = that.x
			this.y = that.y
			this.onCurve = that.onCurve
		}
	}

	enum class Origin {
		TOP, BASELINE
	}

	fun fillText(c: Context2d, text: String, size: Double = 16.0, x: Double = 0.0, y: Double = 0.0, color: Int = Colors.WHITE, origin: Origin = Origin.BASELINE) = c.run {
		val font = this@TtfFont
		val scale = size / unitsPerEm.toDouble()
		translate(x, y)

		for (char in text) {
			val g = getGlyphByChar(char)
			if (g != null) {
				g.fill(this, 32.0, TtfFont.Origin.TOP, Colors.BLUE)
				translate(scale * g.advanceWidth, 0.0)
			}
		}
	}

	data class GlyphReference(
			val glyph: IGlyph,
			val x: Int, val y: Int,
			val scaleX: Float,
			val scale01: Float,
			val scale10: Float,
			val scaleY: Float
	)

	inner class CompositeGlyph(
			override val xMin: Int, override val yMin: Int,
			override val xMax: Int, override val yMax: Int,
			val refs: List<GlyphReference>,
			override val advanceWidth: Int
	) : IGlyph {
		override fun fill(c: Context2d, size: Double, origin: Origin, color: Int) {
			val scale = size / unitsPerEm.toDouble()
			c.keep {
				for (ref in refs) {
					c.keep {
						c.translate((ref.x - xMin) * scale, (-ref.y - yMin) * scale)
						c.scale(ref.scaleX.toDouble(), ref.scaleY.toDouble())
						ref.glyph.fill(c, size, origin, color)
					}
				}
			}
		}
	}

	inner class Glyph(
			override val xMin: Int, override val yMin: Int,
			override val xMax: Int, override val yMax: Int,
			val contoursIndices: IntArray,
			val flags: IntArray,
			val xPos: IntArray,
			val yPos: IntArray,
			override val advanceWidth: Int
	) : IGlyph {
		val npoints: Int get() = xPos.size
		fun onCurve(n: Int) = (flags[n] and 1) != 0
		fun contour(n: Int, out: Contour = Contour()) = out.apply {
			x = xPos[n]
			y = yPos[n]
			onCurve = onCurve(n)
		}

		override fun fill(c: Context2d, size: Double, origin: Origin, color: Int) {
			val font = this@TtfFont
			val scale = size / font.unitsPerEm.toDouble()
			c.apply {
				keep {
					val ydist: Double = when (origin) {
						Origin.TOP -> (font.yMax - font.yMin + yMin).toDouble()
						Origin.BASELINE -> 0.0
					}
					translate(0.0 * scale, (ydist - yMin) * scale)
					scale(scale, -scale)
					beginPath()
					draw(createGraphicsPath())
					fill(com.soywiz.korim.vector.Context2d.Color(color))
				}
			}
		}

		fun createGraphicsPath(): GraphicsPath {
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
	}

	fun SyncStream.readF2DOT14(): Float {
		val v = readS16_be()
		val i = v shr 14
		val f = v and 0x3FFF
		return i.toFloat() + f.toFloat() / 16384f
	}

	fun SyncStream.readMix_BE(signed: Boolean, word: Boolean): Int {
		return when {
			!word && signed -> readS8()
			!word && !signed -> readU8()
			word && signed -> readS16_be()
			word && !signed -> readU16_be()
			else -> invalidOp
		}
	}

	fun SyncStream.readGlyph(index: Int): IGlyph {
		val ncontours = readS16_be()
		val xMin = readS16_be()
		val yMin = readS16_be()
		val xMax = readS16_be()
		val yMax = readS16_be()

		if (ncontours < 0) {
			//println("WARNING: readCompositeGlyph not implemented")

			val ARG_1_AND_2_ARE_WORDS = 0x0001
			val ARGS_ARE_XY_VALUES = 0x0002
			val ROUND_XY_TO_GRID = 0x0004
			val WE_HAVE_A_SCALE = 0x0008
			val MORE_COMPONENTS = 0x0020
			val WE_HAVE_AN_X_AND_Y_SCALE = 0x0040
			val WE_HAVE_A_TWO_BY_TWO = 0x0080
			val WE_HAVE_INSTRUCTIONS = 0x0100
			val USE_MY_METRICS = 0x0200
			val OVERLAP_COMPOUND = 0x0400
			val SCALED_COMPONENT_OFFSET = 0x0800
			val UNSCALED_COMPONENT_OFFSET = 0x1000

			val references = arrayListOf<GlyphReference>()

			do {
				val flags = readU16_be()
				val glyphIndex = readU16_be()
				val signed = (flags and ARGS_ARE_XY_VALUES) != 0
				val words = (flags and ARG_1_AND_2_ARE_WORDS) != 0
				val x = readMix_BE(signed, words)
				val y = readMix_BE(signed, words)
				var scaleX: Float = 1f
				var scaleY: Float = 1f
				var scale01: Float = 0f
				var scale10: Float = 0f

				if ((flags and WE_HAVE_A_SCALE) != 0) {
					scaleX = readF2DOT14()
					scaleY = scaleX
				} else if ((flags and WE_HAVE_AN_X_AND_Y_SCALE) != 0) {
					scaleX = readF2DOT14()
					scaleY = readF2DOT14()
				} else if ((flags and WE_HAVE_A_TWO_BY_TWO) != 0) {
					scaleX = readF2DOT14()
					scale01 = readF2DOT14()
					scale10 = readF2DOT14()
					scaleY = readF2DOT14()
				}
				//val useMyMetrics = flags hasFlag USE_MY_METRICS
				val ref = GlyphReference(getGlyphByIndex(glyphIndex)!!, x, y, scaleX, scale01, scale10, scaleY)
				//println("signed=$signed, words=$words, useMyMetrics=$useMyMetrics")
				//println(ref)
				references += ref
			} while ((flags and MORE_COMPONENTS) != 0)

			return CompositeGlyph(xMin, yMin, xMax, yMax, references, horMetrics[index].advanceWidth)
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
				var p = 0
				for (n in 0 until numPoints) {
					val flag = flags[n]
					val b1 = ((flag ushr (1 + xy)) and 1) != 0
					val b2 = ((flag ushr (4 + xy)) and 1) != 0
					if (b1) {
						val magnitude = readU8()
						if (b2) p += magnitude else p -= magnitude
					} else if (!b2) {
						p += readS16_be()
					}
					pos[n] = p
				}
			}

			//println("$ncontours, $xMin, $yMax, $xMax, $yMax, ${endPtsOfContours.toList()}, $numPoints, ${flags.toList()}")
			//println(xPos.toList())
			//println(yPos.toList())
			return Glyph(xMin, yMin, xMax, yMax, contoursIndices, flags.data.copyOf(flags.size), xPos, yPos, horMetrics[index].advanceWidth)
		}
	}
}

fun Context2d.fillText(font: TtfFont, text: String, size: Double = 16.0, x: Double = 0.0, y: Double = 0.0, color: Int = Colors.WHITE, origin: TtfFont.Origin = TtfFont.Origin.BASELINE) {
	font.fillText(this, text, size, x, y, color, origin)
}
