package com.soywiz.korim.font.ttf

import com.soywiz.kmem.readS16_LEBE
import com.soywiz.kmem.write16_LEBE
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.*
import com.soywiz.korma.ds.IntArrayList

// Used information from:
// - https://www.sweetscape.com/010editor/repository/files/TTF.bt
// - http://scripts.sil.org/cms/scripts/page.php?site_id=nrsi&id=iws-chapter08
class TtfFontReader private constructor(val s: SyncStream) {
	data class Table(val id: String, val checksum: Int, val offset: Int, val length: Int) {
		lateinit var s: SyncStream

		fun open() = s.clone()
	}

	val tablesByName = LinkedHashMap<String, Table>()

	companion object {
		operator fun invoke(s: SyncStream): TtfFontReader {
			return TtfFontReader(s)
		}
	}

	init {
		s.readHeader()
		tryReadNames()
		readLoca()
		readGlyphs()
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
			table.s = s.sliceWithSize(table.offset, table.length)
			tablesByName[table.id] = table
		}

		//for (table in tables) println(table)
	}

	fun tryReadNames() {
		val s = tablesByName["name"]?.open() ?: return
		s.clone().apply {
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
				val string = s.clone().sliceWithSize(stringOffset.toLong() + offset, length.toLong()).readAll().toString(charset)
				//println(string)
			}
		}
	}

	fun readLoca() {
		val s = tablesByName["loca"]?.open() ?: return
		val halfOffsets = s.readCharArray_be(s.length.toInt() / 2)
	}

	fun readGlyphs() {
		val s = tablesByName["glyf"]?.open() ?: return
		while (!s.eof) {
			s.readGlyph()
		}
	}

	fun SyncStream.readGlyph() {
		val ncontours = readU16_be()
		val xMin = readS16_be()
		val yMin = readS16_be()
		val xMax = readS16_be()
		val yMax = readS16_be()
		val endPtsOfContours = readCharArray_be(ncontours)
		val instructionLength = readU16_be()
		if (instructionLength != 0) invalidOp("Unsupported instructions : $instructionLength")
		val nitems = endPtsOfContours.lastOrNull()?.toInt()?.plus(1) ?: 0
		val flags = IntArrayList()
		for (n in 0 until nitems) {
			val cf = readU8()
			flags.add(cf)
			// Repeat
			if ((cf and 8) != 0) {
				val count = readU8()
				for (n in 0 until count) flags.add(cf)
			}
		}
		//println("$ncontours, $xMin, $yMax, $xMax, $yMax, ${endPtsOfContours.toList()}, $nitems, ${compressedFlags.data.hex}")
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
