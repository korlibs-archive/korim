package com.soywiz.korim.font

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(BitmapFontAsyncFactory::class)
class BitmapFont(
	val atlas: Bitmap,
	val fontSize: Int,
	val lineHeight: Int,
	val base: Int,
	val glyphs: IntMap<Glyph>,
	val kernings: IntMap<Kerning>
) : Extra by Extra.Mixin() {
	fun measureWidth(text: String): Int {
		var x = 0
		for (c in text) {
			val glyph = glyphs[c.toInt()]
			if (glyph != null) x += glyph.xadvance
		}
		return x
	}

	fun drawText(bmp: Bitmap32, str: String, x: Int = 0, y: Int, color: RGBA = Colors.WHITE) {
		var py = y
		var px = x
		for (c in str) {
			val g = glyphs[c.toInt()]
			if (g != null) {
				bmp.drawUnoptimized(g.texture, px, py)
				px += g.xadvance
			}
			if (c == '\n') {
				py += lineHeight
				px = x
			}
		}
	}

	fun getKerning(first: Char, second: Char): Kerning? = getKerning(first.toInt(), second.toInt())
	fun getKerning(first: Int, second: Int): Kerning? = kernings[BitmapFont.Kerning.buildKey(first, second)]

	class Kerning(
		val first: Int,
		val second: Int,
		val amount: Int
	) {
		companion object {
			fun buildKey(f: Int, s: Int) = f or (s shl 16)
		}
	}

	class Glyph(
		val id: Int,
		val texture: BitmapSlice<Bitmap>,
		val xoffset: Int,
		val yoffset: Int,
		val xadvance: Int
	)

	val dummyGlyph by lazy { Glyph(-1, Bitmaps.transparent, 0, 0, 0) }
	val anyGlyph: Glyph by lazy { glyphs[glyphs.keys.iterator().next()] ?: dummyGlyph }
	val baseBmp: Bitmap by lazy { anyGlyph.texture.bmp }

	operator fun get(charCode: Int): Glyph = glyphs[charCode] ?: glyphs[32] ?: dummyGlyph
	operator fun get(char: Char): Glyph = this[char.toInt()]

	companion object {
	}
}

suspend fun VfsFile.readBitmapFont(imageFormats: ImageFormats = defaultImageFormats): BitmapFont {
	val fntFile = this
	val content = fntFile.readString().trim()
	val textures = hashMapOf<Int, BitmapSlice<Bitmap>>()

	when {
		// XML
		content.startsWith('<') -> {
			return readBitmapFontXml(content, fntFile, textures, imageFormats)
		}
		// FNT
		content.startsWith("info") -> {
			return readBitmapFontTxt(content, fntFile, textures, imageFormats)
		}
		else -> TODO("Unsupported font type starting with ${content.substr(0, 16)}")
	}
}

private suspend fun readBitmapFontTxt(
	content: String,
	fntFile: VfsFile,
	textures: HashMap<Int, BitmapSlice<Bitmap>>,
	imageFormats: ImageFormats
): BitmapFont {
	data class BmpChar(
		val id: Int, val x: Int, val y: Int, val width: Int, val height: Int,
		val xoffset: Int, var yoffset: Int, val xadvance: Int,
		val page: Int, val chnl: Int
	)

	val kernings = arrayListOf<BitmapFont.Kerning>()
	val glyphs = arrayListOf<BitmapFont.Glyph>()
	var lineHeight = 16
	var fontSize: Int? = null
	var base: Int? = null
	for (rline in content.lines()) {
		val line = rline.trim()
		val map = LinkedHashMap<String, String>()
		for (part in line.split(' ')) {
			val (key, value) = part.split('=') + listOf("", "")
			map[key] = value
		}
		when {
			line.startsWith("info") -> {
				fontSize = map["size"]?.toInt()
			}
			line.startsWith("page") -> {
				val id = map["id"]?.toInt() ?: 0
				val file = map["file"]?.unquote() ?: error("page without file")
				textures[id] = fntFile.parent[file].readBitmapSlice()
			}
			line.startsWith("common ") -> {
				lineHeight = map["lineHeight"]?.toIntOrNull() ?: 16
				base = map["base"]?.toIntOrNull()
			}
			line.startsWith("char ") -> {
				//id=54 x=158 y=88 width=28 height=42 xoffset=2 yoffset=8 xadvance=28 page=0 chnl=0
				val page = map["page"]?.toIntOrNull() ?: 0
				val texture = textures[page] ?: textures.values.first()
				glyphs += Dynamic {
					BitmapFont.Glyph(
						id = map["id"].int,
						xoffset = map["xoffset"].int,
						yoffset = map["yoffset"].int,
						xadvance = map["xadvance"].int,
						texture = texture.sliceWithSize(map["x"].int, map["y"].int, map["width"].int, map["height"].int)
					)
				}
			}
			line.startsWith("kerning ") -> {
				kernings += BitmapFont.Kerning(
					first = map["first"]?.toIntOrNull() ?: 0,
					second = map["second"]?.toIntOrNull() ?: 0,
					amount = map["amount"]?.toIntOrNull() ?: 0
				)
			}
		}
	}
	return BitmapFont(
		atlas = textures.values.first().bmp,
		fontSize = fontSize ?: 16,
		lineHeight = lineHeight,
		base = base ?: lineHeight,
		glyphs = glyphs.map { it.id to it }.toMap().toIntMap(),
		kernings = kernings.map { BitmapFont.Kerning.buildKey(it.first, it.second) to it }.toMap().toIntMap()
	)
}

private suspend fun readBitmapFontXml(
	content: String,
	fntFile: VfsFile,
	textures: MutableMap<Int, BitmapSlice<Bitmap>>,
	imageFormats: ImageFormats = defaultImageFormats
): BitmapFont {
	val xml = Xml(content)

	val fontSize = xml["info"].firstOrNull()?.int("size", 16) ?: 16
	val lineHeight = xml["common"].firstOrNull()?.int("lineHeight", 16) ?: 16
	val base = xml["common"].firstOrNull()?.int("base", 16) ?: 16

	for (page in xml["pages"]["page"]) {
		val id = page.int("id")
		val file = page.str("file")
		val texFile = fntFile.parent[file]
		val tex = texFile.readBitmapSlice()
		textures[id] = tex
	}

	val glyphs = xml["chars"]["char"].map {
		val page = it.int("page")
		val texture = textures[page] ?: textures.values.first()
		BitmapFont.Glyph(
			id = it.int("id"),
			texture = texture.sliceWithSize(it.int("x"), it.int("y"), it.int("width"), it.int("height")),
			xoffset = it.int("xoffset"),
			yoffset = it.int("yoffset"),
			xadvance = it.int("xadvance")
		)
	}

	val kernings = xml["kernings"]["kerning"].map {
		BitmapFont.Kerning(
			first = it.int("first"),
			second = it.int("second"),
			amount = it.int("amount")
		)
	}

	return BitmapFont(
		atlas = textures.values.first().bmp,
		fontSize = fontSize,
		lineHeight = lineHeight,
		base = base,
		glyphs = glyphs.map { it.id to it }.toMap().toIntMap(),
		kernings = kernings.map { BitmapFont.Kerning.buildKey(it.first, it.second) to it }.toMap().toIntMap()
	)
}

fun Bitmap32.drawText(font: BitmapFont, str: String, x: Int = 0, y: Int = 0, color: RGBA = Colors.WHITE) =
	font.drawText(this, str, x, y, color)


/*
class BitmapFont(
	val atlas: Bitmap32,
	val size: Int,
	val lineHeight: Int,
	val glyphInfos: List<GlyphInfo>
) {
	val glyphsById = glyphInfos.map { it.id to Glyph(atlas.slice(it.bounds), it) }.toMap()

	fun measureWidth(text: String): Int {
		var x = 0
		for (c in text) {
			val glyph = glyphsById[c.toInt()]
			if (glyph != null) x += glyph.advance
		}
		return x
	}

	fun drawText(bmp: Bitmap32, str: String, x: Int = 0, y: Int, color: RGBA = Colors.WHITE) {
		var py = y
		var px = x
		for (c in str) {
			val g = glyphsById[c.toInt()]
			if (g != null) {
				bmp.draw(g.bmp, px, py)
				px += g.advance
			}
			if (c == '\n') {
				py += lineHeight
				px = x
			}
		}
	}

	data class Glyph(val bmp: BitmapSlice<Bitmap32>, val info: GlyphInfo) {
		val advance = info.advance
	}

	data class GlyphInfo(val id: Int, val bounds: RectangleInt, val advance: Int)
}
*/