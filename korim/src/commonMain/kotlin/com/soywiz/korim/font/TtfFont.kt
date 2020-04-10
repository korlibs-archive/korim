package com.soywiz.korim.font

import com.soywiz.kds.IntArrayList
import com.soywiz.kds.IntIntMap
import com.soywiz.kmem.extract16Signed
import com.soywiz.kmem.insert
import com.soywiz.kmem.unsigned
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
import com.soywiz.korim.vector.TextMetrics
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.UTF16_BE
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.invalidOp
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.encoding.hex
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.vector.lineTo
import com.soywiz.korma.geom.vector.moveTo
import com.soywiz.korma.geom.vector.quadTo
import kotlin.collections.set
import kotlin.math.max

@Suppress("MemberVisibilityCanBePrivate", "UNUSED_VARIABLE", "LocalVariableName", "unused")
// Used information from:
// - https://www.sweetscape.com/010editor/repository/files/TTF.bt
// - http://scripts.sil.org/cms/scripts/page.php?site_id=nrsi&id=iws-chapter08
// - https://www.microsoft.com/en-us/Typography/OpenTypeSpecification.aspx
// - https://en.wikipedia.org/wiki/Em_(typography)
// - http://stevehanov.ca/blog/index.php?id=143 (Let's read a Truetype font file from scratch)
// - http://chanae.walon.org/pub/ttf/ttf_glyphs.htm
class TtfFont private constructor(val s: SyncStream, override val name: String = "TtfFont", override val registry: FontRegistry = SystemFontRegistry) : Font {

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
    val characterMaps = IntIntMap()

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

    override val size: Double get() = yMax.toDouble()

    override fun getTextBounds(text: String, out: TextMetrics): TextMetrics {
        var maxx = 0.0
        var maxy = 0.0
        commonProcess(text, handleBounds = { _maxx, _maxy ->
            maxx = _maxx
            maxy = _maxy
        })
        return TextMetrics(Rectangle(0, 0, maxx, maxy))
    }
    override fun renderText(ctx: Context2d, text: String, x: Double, y: Double, fill: Boolean) {
        commonProcess(text, handleGlyph = { x, y, g ->
            g.draw(ctx, size, origin = Origin.TOP)
            if (fill) ctx.fill() else ctx.stroke()
        })
    }

    private inline fun commonProcess(
        text: String,
        handleGlyph: (x: Double, y: Double, g: IGlyph) -> Unit = { x, y, g -> },
        handleBounds: (maxx: Double, maxy: Double) -> Unit = { maxx, maxy -> }
    ) {
        var x = 0.0
        var y = 0.0
        var maxx = 0.0
        for (c in text) {
            if (c == '\n') {
                x = 0.0
                y += yMax
            } else {
                val glyph = getGlyphByChar(c)
                if (glyph != null) {
                    handleGlyph(x, y, glyph)
                    x += glyph.advanceWidth
                    maxx = max(maxx, x + glyph.advanceWidth)
                }
            }
        }
        handleBounds(maxx, y + yMax)
    }

    data class Table(val id: String, val checksum: Int, val offset: Int, val length: Int) {
		lateinit var s: SyncStream

		fun open() = s.clone()
	}

	@Suppress("unused")
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

	fun SyncStream.readFixed() = Fixed(readS16LE(), readS16LE())
	data class HorMetric(val advanceWidth: Int, val lsb: Int)

	val tablesByName = LinkedHashMap<String, Table>()
	fun openTable(name: String) = tablesByName[name]?.open()

	companion object {
		operator fun invoke(s: SyncStream): TtfFont {
			return TtfFont(s)
		}
	}

	fun readHeaderTables() = s.sliceStart().apply {
		val majorVersion = readU16BE().apply { if (this != 1) invalidOp("Not a TTF file") }
		val minorVersion = readU16BE().apply { if (this != 0) invalidOp("Not a TTF file") }
		val numTables = readU16BE()
		val searchRange = readU16BE()
		val entrySelector = readU16BE()
		val rangeShift = readU16BE()

		val tables = (0 until numTables).map {
            Table(readStringz(4), readS32BE(), readS32BE(), readS32BE())
		}

		for (table in tables) {
			table.s = sliceWithSize(table.offset, table.length)
			tablesByName[table.id] = table
		}

		//for (table in tables) println(table)
	}

	inline fun runTableUnit(name: String, callback: SyncStream.() -> Unit) {
		openTable(name)?.callback()
	}

	inline fun <T> runTable(name: String, callback: SyncStream.() -> T): T? = openTable(name)?.let { callback(it) }

	fun readNames() = runTableUnit("name") {
		val format = readU16BE()
		val count = readU16BE()
		val stringOffset = readU16BE()
		for (n in 0 until count) {
			val platformId = readU16BE()
			val encodingId = readU16BE()
			val languageId = readU16BE()
			val nameId = readU16BE()
			val length = readU16BE()
			val offset = readU16BE()

			val charset = when (encodingId) {
				0 -> UTF8
				1 -> UTF16_BE
				else -> UTF16_BE
			}
			//println("" + (stringOffset.toLong() + offset) + " : " + length + " : " + charset)
			val string =
				this.clone().sliceWithSize(stringOffset.toLong() + offset, length.toLong()).readAll().toString(charset)
			//println(string)
		}
	}

	fun readLoca() = runTableUnit("loca") {
		val bytesPerEntry = when (indexToLocFormat) {
			0 -> 2
			1 -> 4
			else -> invalidOp
		}

		val data = readBytesExact(bytesPerEntry * (numGlyphs + 1))
		locs = IntArray(numGlyphs + 1)

		FastByteArrayInputStream(data).run {
			when (indexToLocFormat) {
				0 -> run { for (n in locs.indices) locs[n] = readU16BE() * 2 }
				1 -> run { for (n in locs.indices) locs[n] = readS32BE() * 2 }
				else -> invalidOp
			}
		}
		//println("locs: ${locs.toList()}")
	}

	fun readHead() = runTableUnit("head") {
		readU16BE().apply { if (this != 1) invalidOp("Invalid TTF") }
		readU16BE().apply { if (this != 0) invalidOp("Invalid TTF") }
		fontRev = readFixed()
		val checkSumAdjustment = readS32BE()
		readS32BE().apply { if (this != 0x5F0F3CF5) invalidOp("Invalid magic ${this.hex}") }
		val flags = readU16BE()
		unitsPerEm = readU16BE()
		val created = readS64BE() * 1000L
		val modified = readS64BE() * 1000L
		xMin = readS16BE()
		yMin = readS16BE()
		xMax = readS16BE()
		yMax = readS16BE()
		macStyle = readU16BE()
		lowestRecPPEM = readU16BE()
		fontDirectionHint = readS16BE()
		indexToLocFormat = readS16BE() // 0=Int16, 1=Int32
		glyphDataFormat = readS16BE()

		//println("unitsPerEm: $unitsPerEm")
		//println("created: ${DateTime(created) - 76.years}")
		//println("modified: ${DateTime(modified) - 76.years}")
		//println("bounds: ($xMin, $yMin)-($xMax, $yMax)")
	}

	fun readMaxp() = runTableUnit("maxp") {
		val version = readFixed()
		numGlyphs = readU16BE()
		maxPoints = readU16BE()
		maxContours = readU16BE()
		maxCompositePoints = readU16BE()
		maxCompositeContours = readU16BE()
		maxZones = readU16BE()
		maxTwilightPoints = readU16BE()
		maxStorage = readU16BE()
		maxFunctionDefs = readU16BE()
		maxInstructionDefs = readU16BE()
		maxStackElements = readU16BE()
		maxSizeOfInstructions = readU16BE()
		maxComponentElements = readU16BE()
		maxComponentDepth = readU16BE()
	}

	fun readHhea() = runTableUnit("hhea") {
		hheaVersion = readFixed()
		ascender = readS16BE()
		descender = readS16BE()
		lineGap = readS16BE()
		advanceWidthMax = readU16BE()
		minLeftSideBearing = readS16BE()
		minRightSideBearing = readS16BE()
		xMaxExtent = readS16BE()
		caretSlopeRise = readS16BE()
		caretSlopeRun = readS16BE()
		caretOffset = readS16BE()
		readS16BE() // reserved
		readS16BE() // reserved
		readS16BE() // reserved
		readS16BE() // reserved
		metricDataFormat = readS16BE()
		numberOfHMetrics = readU16BE()
	}

	fun readHmtx() = runTableUnit("hmtx") {
		val firstMetrics = (0 until numberOfHMetrics).map {
            HorMetric(
                readU16BE(),
                readS16BE()
            )
        }
		val lastAdvanceWidth = firstMetrics.last().advanceWidth
		val compressedMetrics =
			(0 until (numGlyphs - numberOfHMetrics)).map {
                HorMetric(
                    lastAdvanceWidth,
                    readS16BE()
                )
            }
		horMetrics = firstMetrics + compressedMetrics
	}

	fun readCmap() = runTableUnit("cmap") {
		data class EncodingRecord(val platformId: Int, val encodingId: Int, val offset: Int)

		val version = readU16BE()
		val numTables = readU16BE()
		val tables = (0 until numTables).map { EncodingRecord(readU16BE(), readU16BE(), readS32BE()) }

		for (table in tables) {
			sliceStart(table.offset.toLong()).run {
				val format = readU16BE()
				when (format) {
					4 -> {
						val length = readU16BE()
						//s.readStream(length - 4).run {
						val language = readU16BE()
						val segCount = readU16BE() / 2
						val searchRangeS = readU16BE()
						val entrySelector = readU16BE()
						val rangeShift = readU16BE()
						val endCount = readCharArrayBE(segCount)
						readU16BE() // reserved
						val startCount = readCharArrayBE(segCount)
						val idDelta = readShortArrayBE(segCount)
						val rangeOffsetPos = position.toInt()
						val idRangeOffset = readCharArrayBE(segCount)
						//val glyphIdArray = readCharArrayBE(idRangeOffset.max()?.toInt() ?: 0)

						//println("$language")

						for (n in 0 until segCount) {
							val ec = endCount[n].toInt()
							val sc = startCount[n].toInt()
							val delta = idDelta[n].toInt()
							val iro = idRangeOffset[n].toInt()
							//println("%04X-%04X : %d : %d".format(sc, ec, delta, iro))
							for (c in sc..ec) {
								var index: Int
								if (iro != 0) {
									var glyphIndexOffset = rangeOffsetPos + n * 2
									glyphIndexOffset += iro
									glyphIndexOffset += (c - sc) * 2
									index = sliceStart(glyphIndexOffset.toLong()).readU16BE()
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
						readU16BE() // reserved
						val length = readS32BE()
						val language = readS32BE()
						val numGroups = readS32BE()

						for (n in 0 until numGroups) {
							val startCharCode = readS32BE()
							val endCharCode = readS32BE()
							val startGlyphId = readS32BE()

							var glyphId = startGlyphId
							for (c in startCharCode..endCharCode) {
								characterMaps[c] = glyphId++
							}
						}
					}
					else -> { // Ignored

					}
				}
				//println("cmap.table.format: $format")
			}
		}
		//println(tables)
	}

	fun getCharIndexFromCodePoint(codePoint: Int): Int? = characterMaps[codePoint].takeIf { it >= 0 }
	fun getCharIndexFromChar(char: Char): Int? = characterMaps[char.toInt()].takeIf { it >= 0 }

	fun getGlyphByCodePoint(codePoint: Int): IGlyph? = if (codePoint in characterMaps) {
        getGlyphByIndex(characterMaps[codePoint])
    } else {
        null
    }
	fun getGlyphByChar(char: Char): IGlyph? = getGlyphByCodePoint(char.toInt())

    operator fun get(char: Char) = getGlyphByChar(char)
    operator fun get(codePoint: Int) = getGlyphByCodePoint(codePoint)

	fun getGlyphByIndex(index: Int): IGlyph? = runTable("glyf") {
		val start = locs.getOrNull(index)?.unsigned ?: 0
		val end = locs.getOrNull(index + 1)?.unsigned ?: start
		val size = end - start
		if (size != 0L) {
			sliceStart(start).readGlyph(index)
		} else {
			Glyph(0, 0, 0, 0, intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf(), horMetrics[index].advanceWidth)
		}
	}

	fun getAllGlyphs() = (0 until numGlyphs).mapNotNull { getGlyphByIndex(it) }

	interface IGlyph {
		val xMin: Int
		val yMin: Int
		val xMax: Int
		val yMax: Int
		val advanceWidth: Int
		fun draw(c: Context2d, size: Double, origin: Origin)
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

	fun fillText(
		c: Context2d,
		text: String,
		size: Double = 16.0,
		x: Double = 0.0,
		y: Double = 0.0,
		color: RGBA = Colors.WHITE,
		@Suppress("UNUSED_PARAMETER") origin: Origin = Origin.BASELINE
	) = c.run {
		val font = this@TtfFont
		val scale = size / unitsPerEm.toDouble()
		translate(x, y)

		for (char in text) {
			val g = getGlyphByChar(char)
			if (g != null) {
                c.fill(Context2d.Color(color)) {
                    g.draw(this, size, Origin.TOP)
                }
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
		override fun draw(c: Context2d, size: Double, origin: Origin) {
			val scale = size / unitsPerEm.toDouble()
			c.keep {
				for (ref in refs) {
					c.keep {
						c.translate((ref.x - xMin) * scale, (-ref.y - yMin) * scale)
						c.scale(ref.scaleX.toDouble(), ref.scaleY.toDouble())
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

		override fun draw(c: Context2d, size: Double, origin: Origin) {
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
		val v = readS16BE()
		val i = v shr 14
		val f = v and 0x3FFF
		return i.toFloat() + f.toFloat() / 16384f
	}

	@Suppress("FunctionName")
	fun SyncStream.readMixBE(signed: Boolean, word: Boolean): Int {
		return when {
			!word && signed -> readS8()
			!word && !signed -> readU8()
			word && signed -> readS16BE()
			word && !signed -> readU16BE()
			else -> invalidOp
		}
	}

	fun SyncStream.readGlyph(index: Int): IGlyph {
		val ncontours = readS16BE()
		val xMin = readS16BE()
		val yMin = readS16BE()
		val xMax = readS16BE()
		val yMax = readS16BE()

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
				val flags = readU16BE()
				val glyphIndex = readU16BE()
				val signed = (flags and ARGS_ARE_XY_VALUES) != 0
				val words = (flags and ARG_1_AND_2_ARE_WORDS) != 0
				val x = readMixBE(signed, words)
				val y = readMixBE(signed, words)
				var scaleX = 1f
				var scaleY = 1f
				var scale01 = 0f
				var scale10 = 0f

				when {
					(flags and WE_HAVE_A_SCALE) != 0 -> {
						scaleX = readF2DOT14()
						scaleY = scaleX
					}
					(flags and WE_HAVE_AN_X_AND_Y_SCALE) != 0 -> {
						scaleX = readF2DOT14()
						scaleY = readF2DOT14()
					}
					(flags and WE_HAVE_A_TWO_BY_TWO) != 0 -> {
						scaleX = readF2DOT14()
						scale01 = readF2DOT14()
						scale10 = readF2DOT14()
						scaleY = readF2DOT14()
					}
				}

				//val useMyMetrics = flags hasFlag USE_MY_METRICS
				val ref = GlyphReference(
                    getGlyphByIndex(glyphIndex)!!,
                    x,
                    y,
                    scaleX,
                    scale01,
                    scale10,
                    scaleY
                )
				//println("signed=$signed, words=$words, useMyMetrics=$useMyMetrics")
				//println(ref)
				references += ref
			} while ((flags and MORE_COMPONENTS) != 0)

			return CompositeGlyph(xMin, yMin, xMax, yMax, references, horMetrics[index].advanceWidth)
		} else {
			val contoursIndices = IntArray(ncontours + 1)
			contoursIndices[0] = -1
			for (n in 1..ncontours) contoursIndices[n] = readU16BE()
			val instructionLength = readU16BE()
			val instructions = readBytesExact(instructionLength)
			val numPoints = contoursIndices.lastOrNull()?.plus(1) ?: 0
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
						p += readS16BE()
					}
					pos[n] = p
				}
			}

			//println("$ncontours, $xMin, $yMax, $xMax, $yMax, ${endPtsOfContours.toList()}, $numPoints, ${flags.toList()}")
			//println(xPos.toList())
			//println(yPos.toList())
			return Glyph(
				xMin, yMin,
				xMax, yMax,
				contoursIndices,
				flags.data.copyOf(flags.size),
				xPos, yPos,
				horMetrics[index].advanceWidth
			)
		}
	}
}

fun Context2d.fillText(
    font: TtfFont,
    text: String,
    size: Double = 16.0,
    x: Double = 0.0,
    y: Double = 0.0,
    color: RGBA = Colors.WHITE,
    origin: TtfFont.Origin = TtfFont.Origin.BASELINE
) {
	font.fillText(this, text, size, x, y, color, origin)
}

internal inline class Fixed(val data: Int) {
    val num: Int get() = data.extract16Signed(0)
    val den: Int get() = data.extract16Signed(16)
    companion object {
        operator fun invoke(num: Int, den: Int) = 0.insert(num, 0, 16).insert(den, 16, 16)
    }
}

suspend fun VfsFile.readTtfFont() = TtfFont(this.readAll().openSync())
