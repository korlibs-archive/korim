package com.soywiz.korim.font

import com.soywiz.kds.*
import com.soywiz.klock.measureTimeWithResult
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.HorizontalAlign
import com.soywiz.korim.vector.TextMetrics
import com.soywiz.korim.vector.VerticalAlign
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.RectangleInt
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.max

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(BitmapFontAsyncFactory::class)
class BitmapFont(
    val atlas: Bitmap,
    val fontSize: Int,
    val lineHeight: Int,
    val base: Int,
    val glyphs: IntMap<Glyph>,
    val kernings: IntMap<Kerning>,
    override val name: String = "BitmapFont",
    override val registry: FontRegistry = SystemFontRegistry
) : Extra by Extra.Mixin(), Font {
    override val size get() = fontSize.toDouble()

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
        val metrics = getTextBounds(text)
        val bmpAlpha = Bitmap32(metrics.width.toInt(), metrics.height.toInt())
        commonProcess(text, handleGlyph = { x, y, g ->
            bmpAlpha.draw(g.bmp, (x - metrics.left).toInt(), (y - metrics.top).toInt())
        })
        if (ctx.fillStyle == Context2d.DefaultPaint) {
            ctx.drawImage(bmpAlpha, metrics.left, metrics.top)
        } else {
            val bmpFill = Bitmap32(metrics.width.toInt(), metrics.height.toInt())
            bmpFill.context2d {
                this.fillStyle = ctx.fillStyle
                fillRect(0, 0, width, height)
            }
            bmpFill.writeChannel(BitmapChannel.ALPHA, bmpAlpha, BitmapChannel.ALPHA)
            ctx.drawImage(bmpFill, metrics.left, metrics.top)
        }
    }
    private inline fun commonProcess(
        text: String,
        handleGlyph: (x: Double, y: Double, g: Glyph) -> Unit = { x, y, g -> },
        handleBounds: (maxx: Double, maxy: Double) -> Unit = { maxx, maxy -> }
    ) {
        var x = 0.0
        var y = 0.0
        var maxx = 0.0
        for (c in text) {
            if (c == '\n') {
                x = 0.0
                y += lineHeight
            } else {
                val glyph = this[c]
                handleGlyph(x, y, glyph)
                x += glyph.xadvance
                maxx = max(maxx, x + glyph.xadvance)
            }
        }
        handleBounds(maxx, y + lineHeight)
    }

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
	fun getKerning(first: Int, second: Int): Kerning? = kernings[Kerning.buildKey(
        first,
        second
    )]

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
	) {
        val bmp = texture.extract().toBMP32()
    }

	val dummyGlyph by lazy {
        Glyph(
            -1,
            Bitmaps.transparent,
            0,
            0,
            0
        )
    }
	val anyGlyph: Glyph by lazy { glyphs[glyphs.keys.iterator().next()] ?: dummyGlyph }
	val baseBmp: Bitmap by lazy { anyGlyph.texture.bmp }

	operator fun get(charCode: Int): Glyph = glyphs[charCode] ?: glyphs[32] ?: dummyGlyph
	operator fun get(char: Char): Glyph = this[char.toInt()]

	companion object {
        operator fun invoke(
            fontName: String,
            fontSize: Int,
            chars: String = BitmapFontGenerator.LATIN_ALL,
            mipmaps: Boolean = true
        ): BitmapFont =
            BitmapFontGenerator.generate(fontName, fontSize, chars, mipmaps)

        operator fun invoke(
            font: Font,
            chars: String = BitmapFontGenerator.LATIN_ALL,
            mipmaps: Boolean = true
        ): BitmapFont =
            BitmapFontGenerator.generate(
                font,
                chars.map { it.toInt() }.toIntArray(),
                mipmaps
            )
	}
}

suspend fun VfsFile.readBitmapFont(imageFormat: ImageFormat = RegisteredImageFormats): BitmapFont {
	val fntFile = this
	val content = fntFile.readString().trim()
	val textures = hashMapOf<Int, BitmapSlice<Bitmap>>()

    return when {
        content.startsWith('<') -> readBitmapFontXml(
            content,
            fntFile,
            textures,
            imageFormat
        ) // XML
        content.startsWith("info") -> readBitmapFontTxt(
            content,
            fntFile,
            textures,
            imageFormat
        ) // FNT
        else -> TODO("Unsupported font type starting with ${content.substr(0, 16)}")
    }
}

private suspend fun readBitmapFontTxt(
	content: String,
	fntFile: VfsFile,
	textures: HashMap<Int, BitmapSlice<Bitmap>>,
	imageFormat: ImageFormat = RegisteredImageFormats
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
				textures[id] = fntFile.parent[file].readBitmapSlice(imageFormat)
			}
			line.startsWith("common ") -> {
				lineHeight = map["lineHeight"]?.toIntOrNull() ?: 16
				base = map["base"]?.toIntOrNull()
			}
			line.startsWith("char ") -> {
				//id=54 x=158 y=88 width=28 height=42 xoffset=2 yoffset=8 xadvance=28 page=0 chnl=0
				val page = map["page"]?.toIntOrNull() ?: 0
				val texture = textures[page] ?: textures.values.first()
				glyphs += KDynamic {
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
        kernings = kernings.map {
            BitmapFont.Kerning.buildKey(
                it.first,
                it.second
            ) to it
        }.toMap().toIntMap()
    )
}

private suspend fun readBitmapFontXml(
	content: String,
	fntFile: VfsFile,
	textures: MutableMap<Int, BitmapSlice<Bitmap>>,
    imageFormat: ImageFormat = RegisteredImageFormats
): BitmapFont {
	val xml = Xml(content)

	val fontSize = xml["info"].firstOrNull()?.int("size", 16) ?: 16
	val lineHeight = xml["common"].firstOrNull()?.int("lineHeight", 16) ?: 16
	val base = xml["common"].firstOrNull()?.int("base", 16) ?: 16

	for (page in xml["pages"]["page"]) {
		val id = page.int("id")
		val file = page.str("file")
		val texFile = fntFile.parent[file]
		val tex = texFile.readBitmapSlice(imageFormat)
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
        kernings = kernings.map {
            BitmapFont.Kerning.buildKey(
                it.first,
                it.second
            ) to it
        }.toMap().toIntMap()
    )
}

fun Bitmap32.drawText(font: BitmapFont, str: String, x: Int = 0, y: Int = 0, color: RGBA = Colors.WHITE) =
	font.drawText(this, str, x, y, color)

object BitmapFontGenerator {
    val SPACE = " "
    val UPPERCASE = ('A'..'Z').joinToString("")
    val LOWERCASE = ('a'..'z').joinToString("")
    val NUMBERS = ('0'..'9').joinToString("")
    val PUNCTUATION = "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}"
    val LATIN_BASIC = "ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥PÉáíóúñÑª°¿¬½¼¡«»ßµø±÷°·.²"
    val LATIN_ALL = SPACE + UPPERCASE + LOWERCASE + NUMBERS + PUNCTUATION + LATIN_BASIC

    fun generate(fontName: String, fontSize: Number, chars: String, mipmaps: Boolean = true, fontRegistry: FontRegistry = SystemFontRegistry): BitmapFont =
        generate(
            fontRegistry.get(
                fontName,
                fontSize.toDouble()
            ), chars.indices.map { chars[it].toInt() }.toIntArray(), mipmaps
        )

    fun generate(fontName: String, fontSize: Number, chars: IntArray, mipmaps: Boolean = true, fontRegistry: FontRegistry = SystemFontRegistry): BitmapFont =
        generate(
            fontRegistry.get(
                fontName,
                fontSize.toDouble()
            ), chars.indices.map { chars[it].toInt() }.toIntArray(), mipmaps
        )

    fun generate(font: Font, chars: IntArray, mipmaps: Boolean = true, name: String = font.name): BitmapFont {
        val result = measureTimeWithResult {
            val bni = NativeImage(1, 1)
            val bnictx = bni.getContext2d()
            bnictx.font = font
            val bitmapHeight = bnictx.getTextBounds("a").bounds.height.toInt()

            val widths: List<Int> = chars.map { bnictx.getTextBounds("${it.toChar()}").bounds.width.toInt() }
            val widthsSum = widths.map { it + 2 }.sum()
            val ni = NativeImage(widthsSum, bitmapHeight)

            class GlyphInfo(val char: Int, val rect: RectangleInt, val width: Int)

            val g = ni.getContext2d()
            g.fillStyle = g.createColor(Colors.WHITE)
            g.font = font
            g.horizontalAlign = HorizontalAlign.LEFT
            g.verticalAlign = VerticalAlign.TOP
            val glyphsInfo = arrayListOf<GlyphInfo>()
            var x = 0
            val itemp = IntArray(1)
            for ((index, char) in chars.withIndex()) {
                val width = widths[index]
                itemp[0] = char
                g.fillText(String_fromIntArray(itemp, 0, 1), x.toDouble(), 0.0)
                glyphsInfo += GlyphInfo(char, RectangleInt(x, 0, width, ni.height), width)
                x += width + 2
            }

            val atlas = ni.toBMP32()

            BitmapFont(
                atlas, font.size.toInt(), font.size.toInt(), font.size.toInt(),
                glyphsInfo.associate {
                    it.char to BitmapFont.Glyph(
                        it.char,
                        atlas.slice(it.rect),
                        0,
                        0,
                        it.width
                    )
                }.toIntMap(),
                IntMap(),
                name = name
            )
        }

        return result.result
    }
}

